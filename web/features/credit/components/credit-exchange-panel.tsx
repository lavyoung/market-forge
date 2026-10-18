"use client";

import {
    CreditExchangeOutcomeUnknownError,
    exchangeCreditForSku,
    queryActivitySkuProducts,
    queryCreditAccount,
} from "@/features/credit/api/credit-api";
import type {ActivitySkuProduct, CreditAccount} from "@/features/credit/model/credit-types";
import {useCallback, useEffect, useRef, useState} from "react";
import styles from "./credit-exchange-panel.module.css";

type CreditExchangePanelProps = {
    activityId: number;
    externalRefreshToken: number;
    onExchangeAccepted: () => Promise<boolean> | boolean;
};

type Notice = {
    kind: "info" | "success" | "error" | "unknown";
    message: string;
};

/**
 * 积分或活动额度异步变化后的查询间隔。
 *
 * 查询次数必须有限，既为 RabbitMQ 消费和事务提交保留时间，也避免服务异常时
 * 浏览器持续请求。数组首项为零，表示业务完成后先立即读取一次最新状态。
 */
const CREDIT_REFRESH_DELAYS = [0, 400, 800, 1200] as const;

const NUMBER_FORMATTER = new Intl.NumberFormat("zh-CN");

/**
 * 等待指定时长。
 *
 * @param delayMs 等待毫秒数
 * @returns 等待完成后的 Promise
 */
function wait(delayMs: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, delayMs));
}

/**
 * 展示当前用户积分余额和活动 SKU 兑换商品。
 *
 * 组件负责查询积分账户、加载商品、提交单次兑换，以及在兑换或积分奖品发放后
 * 有限次数刷新余额。兑换结果未知时会锁定后续兑换操作，防止用户生成新的幂等号
 * 重复扣减积分；活动抽奖额度的异步刷新则通过 onExchangeAccepted 交给父页面处理。
 *
 * @param props 当前活动、外部积分刷新信号和兑换后额度刷新回调
 * @returns 积分账户与商品兑换操作区域
 */
export default function CreditExchangePanel({
                                                activityId,
                                                externalRefreshToken,
                                                onExchangeAccepted
                                            }: CreditExchangePanelProps) {
    const [account, setAccount] = useState<CreditAccount | null>(null);
    const [products, setProducts] = useState<ActivitySkuProduct[]>([]);
    const [accountError, setAccountError] = useState<string | null>(null);
    const [productsError, setProductsError] = useState<string | null>(null);
    const [accountLoading, setAccountLoading] = useState(true);
    const [productsLoading, setProductsLoading] = useState(true);
    const [activeSku, setActiveSku] = useState<number | null>(null);
    const [exchangeBlocked, setExchangeBlocked] = useState(false);
    const [notice, setNotice] = useState<Notice | null>(null);

    const mountedRef = useRef(false);
    const exchangeInFlightRef = useRef(false);
    const accountRef = useRef<CreditAccount | null>(null);
    const handledRefreshTokenRef = useRef(0);

    /**
     * 同时更新积分账户状态和 ref 快照。
     *
     * ref 供异步兑换流程读取最新余额，避免事件处理函数闭包持有旧账户数据。
     *
     * @param nextAccount 最新积分账户
     */
    const updateAccount = useCallback((nextAccount: CreditAccount): void => {
        accountRef.current = nextAccount;
        setAccount(nextAccount);
        setAccountError(null);
    }, []);

    useEffect(() => {
        mountedRef.current = true;
        const controller = new AbortController();

        void queryCreditAccount(controller.signal)
            .then((nextAccount) => {
                if (!controller.signal.aborted && mountedRef.current) {
                    updateAccount(nextAccount);
                }
            })
            .catch((error: unknown) => {
                if (!controller.signal.aborted && mountedRef.current) {
                    setAccountError(error instanceof Error ? error.message : "积分账户加载失败");
                }
            })
            .finally(() => {
                if (!controller.signal.aborted && mountedRef.current) {
                    setAccountLoading(false);
                }
            });

        void queryActivitySkuProducts(activityId, controller.signal)
            .then((nextProducts) => {
                if (!controller.signal.aborted && mountedRef.current) {
                    setProducts(nextProducts);
                    setProductsError(null);
                }
            })
            .catch((error: unknown) => {
                if (!controller.signal.aborted && mountedRef.current) {
                    setProductsError(error instanceof Error ? error.message : "积分兑换商品加载失败");
                }
            })
            .finally(() => {
                if (!controller.signal.aborted && mountedRef.current) {
                    setProductsLoading(false);
                }
            });

        return () => {
            mountedRef.current = false;
            controller.abort();
        };
    }, [activityId, updateAccount]);

    /**
     * 在积分调整后有限次数查询账户，直到余额发生变化。
     *
     * @param previousAmount 业务发生前的积分余额；首次加载失败时可为空
     * @returns 查询到余额变化返回 true，查询成功但暂未变化返回 false
     * @throws Error 所有查询都失败时抛出最后一次异常
     */
    const refreshCreditUntilChanged = useCallback(async (previousAmount: number | null): Promise<boolean> => {
        let lastError: unknown;
        let refreshed = false;

        for (const delayMs of CREDIT_REFRESH_DELAYS) {
            if (delayMs > 0) {
                await wait(delayMs);
            }

            if (!mountedRef.current) {
                return false;
            }

            try {
                const nextAccount = await queryCreditAccount();

                if (!mountedRef.current) {
                    return false;
                }

                refreshed = true;
                updateAccount(nextAccount);

                if (previousAmount === null || nextAccount.availableAmount !== previousAmount) {
                    return true;
                }
            } catch (error) {
                lastError = error;
            }
        }

        if (!refreshed) {
            throw lastError instanceof Error ? lastError : new Error("积分账户暂时无法刷新");
        }

        return false;
    }, [updateAccount]);

    useEffect(() => {
        if (externalRefreshToken <= 0 || externalRefreshToken === handledRefreshTokenRef.current) {
            return;
        }

        handledRefreshTokenRef.current = externalRefreshToken;
        const previousAmount = accountRef.current?.availableAmount ?? null;

        void refreshCreditUntilChanged(previousAmount)
            .then((changed) => {
                if (!mountedRef.current) {
                    return;
                }

                setNotice(changed
                    ? {kind: "success", message: "积分奖品已经发放到账。"}
                    : {kind: "info", message: "积分奖品已经受理，余额仍在异步同步中。"});
            })
            .catch(() => {
                if (mountedRef.current) {
                    setNotice({kind: "error", message: "积分奖品已经产生，但余额刷新失败，请稍后刷新页面查看。"});
                }
            });
    }, [externalRefreshToken, refreshCreditUntilChanged]);

    /**
     * 重新查询积分账户，供首次加载失败后的用户主动恢复。
     *
     * @returns 查询完成后的 Promise
     */
    async function reloadAccount(): Promise<void> {
        setAccountLoading(true);
        setAccountError(null);

        try {
            updateAccount(await queryCreditAccount());
        } catch (error) {
            if (mountedRef.current) {
                setAccountError(error instanceof Error ? error.message : "积分账户加载失败");
            }
        } finally {
            if (mountedRef.current) {
                setAccountLoading(false);
            }
        }
    }

    /**
     * 重新查询当前活动的兑换商品。
     *
     * @returns 查询完成后的 Promise
     */
    async function reloadProducts(): Promise<void> {
        setProductsLoading(true);
        setProductsError(null);

        try {
            const nextProducts = await queryActivitySkuProducts(activityId);

            if (mountedRef.current) {
                setProducts(nextProducts);
            }
        } catch (error) {
            if (mountedRef.current) {
                setProductsError(error instanceof Error ? error.message : "积分兑换商品加载失败");
            }
        } finally {
            if (mountedRef.current) {
                setProductsLoading(false);
            }
        }
    }

    /**
     * 提交一次积分兑换并刷新积分、库存和活动抽奖额度。
     *
     * 每次用户操作只生成一个 UUID。请求明确失败时允许用户稍后重新操作；
     * 网络中断或成功响应结构异常时无法判断服务端是否已经扣款，组件会锁定兑换，
     * 要求用户先刷新页面核对账户，不能生成新的请求继续提交。
     *
     * @param product 用户选择的活动 SKU 商品
     * @returns 兑换和后续状态刷新完成后的 Promise
     */
    async function handleExchange(product: ActivitySkuProduct): Promise<void> {
        const currentAccount = accountRef.current;
        if (exchangeInFlightRef.current
            || exchangeBlocked
            || !currentAccount
            || product.stockCountSurplus <= 0
            || currentAccount.availableAmount < product.creditAmount) {
            return;
        }

        exchangeInFlightRef.current = true;
        setActiveSku(product.sku);
        setNotice({
            kind: "info",
            message: `正在使用 ${NUMBER_FORMATTER.format(product.creditAmount)} 积分兑换，请不要重复操作……`
        });

        try {
            const orderId = await exchangeCreditForSku(product.sku, crypto.randomUUID());

            if (!mountedRef.current) {
                return;
            }

            setNotice({kind: "info", message: "兑换订单已经创建，正在同步积分余额和抽奖额度……"});

            const [creditResult, quotaResult] = await Promise.allSettled([
                refreshCreditUntilChanged(currentAccount.availableAmount),
                onExchangeAccepted(),
            ]);

            if (!mountedRef.current) {
                return;
            }

            await reloadProducts();

            const refreshFailed = creditResult.status === "rejected" || quotaResult.status === "rejected";
            const creditPending = creditResult.status === "fulfilled" && !creditResult.value;
            const quotaPending = quotaResult.status === "fulfilled" && !quotaResult.value;

            if (refreshFailed) {
                setNotice({kind: "error", message: `兑换订单 ${orderId} 已创建，但账户刷新失败，请稍后刷新页面核实。`});
            } else if (creditPending || quotaPending) {
                setNotice({kind: "info", message: `兑换订单 ${orderId} 已受理，积分或抽奖额度仍在同步中。`});
            } else {
                setNotice({kind: "success", message: `兑换成功，订单 ${orderId} 的积分余额和抽奖额度已刷新。`});
            }
        } catch (error) {
            if (!mountedRef.current) {
                return;
            }

            if (error instanceof CreditExchangeOutcomeUnknownError) {
                setExchangeBlocked(true);
                setNotice({kind: "unknown", message: error.message});
            } else {
                setNotice({kind: "error", message: error instanceof Error ? error.message : "积分兑换失败"});
            }
        } finally {
            exchangeInFlightRef.current = false;

            if (mountedRef.current) {
                setActiveSku(null);
            }
        }
    }

    return (
        <section className={styles.panel} aria-labelledby="credit-exchange-title">
            <div className={styles.accountHeading}>
                <div>
                    <h2 id="credit-exchange-title">积分兑换</h2>
                    <p>使用积分兑换活动抽奖次数</p>
                </div>

                <div className={styles.balance} aria-live="polite">
                    <strong>{account ? NUMBER_FORMATTER.format(account.availableAmount) : "--"}</strong>
                    <span>可用积分</span>
                </div>
            </div>

            {accountLoading && <p className={styles.loading} role="status">正在加载积分账户……</p>}

            {!accountLoading && accountError && (
                <div className={styles.inlineError} role="alert">
                    <span>{accountError}</span>
                    <button type="button" onClick={() => void reloadAccount()}>重新查询</button>
                </div>
            )}

            <div className={styles.productHeading}>
                <h3>兑换商品</h3>
                <button type="button" className={styles.refreshButton} disabled={productsLoading || activeSku !== null}
                        onClick={() => void reloadProducts()}>
                    刷新商品
                </button>
            </div>

            {productsLoading && <p className={styles.loading} role="status">正在加载兑换商品……</p>}

            {!productsLoading && productsError && (
                <div className={styles.inlineError} role="alert">
                    <span>{productsError}</span>
                    <button type="button" onClick={() => void reloadProducts()}>重新加载</button>
                </div>
            )}

            {!productsLoading && !productsError && products.length === 0 && (
                <p className={styles.empty}>当前活动尚未配置积分兑换商品。</p>
            )}

            {products.length > 0 && (
                <ul className={styles.productList} aria-busy={activeSku !== null}>
                    {products.map((product) => {
                        const soldOut = product.stockCountSurplus <= 0;
                        const insufficient = account !== null && account.availableAmount < product.creditAmount;
                        const exchanging = activeSku === product.sku;
                        const disabled = exchangeBlocked || productsLoading || Boolean(productsError) || activeSku !== null || !account || soldOut || insufficient;
                        const buttonText = exchanging ? "兑换处理中……" : soldOut ? "库存不足" : insufficient ? "积分不足" : "立即兑换";

                        return (
                            <li key={product.sku} className={styles.productItem}>
                                <div className={styles.productMain}>
                                    <div>
                                        <strong>{NUMBER_FORMATTER.format(product.totalCount)} 次抽奖机会</strong>
                                        <span>SKU {product.sku}</span>
                                    </div>
                                    <div className={styles.price}>
                                        <strong>{NUMBER_FORMATTER.format(product.creditAmount)}</strong>
                                        <span>积分</span>
                                    </div>
                                </div>

                                <dl className={styles.productDetails}>
                                    <div>
                                        <dt>每日增加</dt>
                                        <dd>{product.dayCount} 次</dd>
                                    </div>
                                    <div>
                                        <dt>每月增加</dt>
                                        <dd>{product.monthCount} 次</dd>
                                    </div>
                                    <div>
                                        <dt>剩余库存</dt>
                                        <dd>{NUMBER_FORMATTER.format(product.stockCountSurplus)}</dd>
                                    </div>
                                </dl>

                                <button type="button" className={styles.exchangeButton} disabled={disabled}
                                        onClick={() => void handleExchange(product)}>
                                    {buttonText}
                                </button>
                            </li>
                        );
                    })}
                </ul>
            )}

            {notice && (
                <p className={`${styles.notice} ${styles[notice.kind]}`}
                   role={notice.kind === "error" || notice.kind === "unknown" ? "alert" : "status"}>
                    {notice.message}
                </p>
            )}
        </section>
    );
}
