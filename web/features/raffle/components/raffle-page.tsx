"use client"


import {ActivityAccount, ActivityAward, ActivityDrawResult} from "@/features/raffle/model/activity-raffle-types";
import {
    armoryActivityRaffle,
    drawActivityRaffle,
    queryActivityAccount,
    queryActivityAwards
} from "@/features/raffle/api/activity-raffle-api";
import ActivityAccountPanel from "@/features/raffle/components/activity-account-panel";
import CalendarSignPanel from "@/features/raffle/components/calendar-sign-panel";
import StrategyWeightPanel from "@/features/raffle/components/strategy-weight-panel";
import {describeUnlockState, normalizeAwards} from "@/features/raffle/model/raffle-award-view";
import Link from "next/link";
import {useEffect, useRef, useState} from "react";
import styles from "./raffle-page.module.css";

type RafflePageProp = {
    activityId: number;
};

type PageData = {
    awards: ActivityAward[];
    error: string | null;
};

/**
 * 活动额度在页面中的独立加载状态。
 *
 * 额度与奖品分别保存错误，避免其中一个接口失败后覆盖另一个接口的数据。
 */
type AccountData = {
    account: ActivityAccount | null;
    error: string | null;
};

type DrawOutcome = {
    result: ActivityDrawResult;
    awardTitle: string;
};

type Phase = "idle" | "requesting" | "animating" | "refreshing";
type ArmoryStatus = "idle" | "assembling" | "success" | "error";

/**
 * 签到后重新查询活动额度的等待间隔。
 *
 * 第一次立即查询，后续查询为异步 MQ 结算预留短暂时间。
 * 次数必须有限，避免后端异常时浏览器无限轮询。
 */
const ACCOUNT_REFRESH_DELAYS = [0, 400, 800, 1200] as const;

/**
 * 等待指定时长后继续执行。
 *
 * @param delayMs 等待的毫秒数
 * @returns 等待结束后完成的 Promise
 */
function wait(delayMs: number): Promise<void> {
    return new Promise((resolve) => setTimeout(resolve, delayMs));
}

/**
 * 播放展示动画，最终停在后端指定的奖品位置。
 *
 * 动画不决定中奖结果，也不请求后端。
 * 返回清理函数，用于页面退出时停止动画。
 *
 * @param count 奖品数量
 * @param targetIndex 服务端中奖结果对应的奖品位置
 * @param onStep 每次动画移动时执行的回调
 * @param onComplete 动画抵达目标位置后执行的回调
 * @returns 停止当前动画并清理定时器的函数
 */
function playAnimation(count: number, targetIndex: number, onStep: (index: number) => void, onComplete: () => void): () => void {
    let step = 0;
    let stopped = false;
    let timer: ReturnType<typeof setTimeout> | undefined;

    const totalStep = count * 3 + targetIndex + 1;

    function tick() {
        if (stopped) {
            return;
        }

        onStep(step % count);
        step += 1;

        if (step >= totalStep) {
            onComplete();
            return;
        }

        const progress = step / totalStep;
        const delay = 45 + Math.pow(progress, 3) * 180;

        timer = setTimeout(tick, delay)
    }

    tick();

    return () => {
        stopped = true;

        if (timer !== undefined) {
            clearTimeout(timer)
        }
    }
}

export default function RafflePage({activityId}: RafflePageProp) {

    const [pageData, setPageData] = useState<PageData | null>(null);
    const [accountData, setAccountData] = useState<AccountData>({account: null, error: null});
    const [phase, setPhase] = useState<Phase>("idle");
    const [activeIndex, setActiveIndex] = useState<number | null>(null);
    const [outcome, setOutcome] = useState<DrawOutcome | null>(null);
    const [drawError, setDrawError] = useState<string | null>(null);
    const [armoryStatus, setArmoryStatus] = useState<ArmoryStatus>("idle");
    const [armoryMessage, setArmoryMessage] = useState<string | null>(null);

    const mountedRef = useRef(false);
    const inFlightRef = useRef(false);
    const stopAnimationRef = useRef<(() => void) | null>(null);

    const awards = pageData?.awards ?? [];
    const isLoading = pageData === null;
    const loadError = pageData?.error ?? null;
    const isDrawing = phase !== "idle";
    const isAssembling = armoryStatus === "assembling";
    const availableCount = accountData.account
        ? Math.min(accountData.account.totalCountSurplus, accountData.account.monthCountSurplus, accountData.account.dayCountSurplus)
        : null;
    const isAccountLoading = accountData.account === null && accountData.error === null;
    const isQuotaExhausted = availableCount === 0;


    useEffect(() => {
        mountedRef.current = true;

        return () => {
            mountedRef.current = false;
            stopAnimationRef.current?.();
        };
    }, []);


    useEffect(() => {
        const controller = new AbortController();

        async function loadAwards() {
            try {
                const data = await queryActivityAwards(activityId, controller.signal);

                if (!controller.signal.aborted) {
                    setPageData({awards: normalizeAwards(data), error: null});
                }
            } catch (error) {
                if (!controller.signal.aborted) {
                    setPageData({
                        awards: [],
                        error: error instanceof Error ? error.message : "活动奖品列表加载失败",
                    });
                }
            }
        }

        async function loadAccount() {
            try {
                const account = await queryActivityAccount(activityId, controller.signal);

                if (!controller.signal.aborted) {
                    setAccountData({account, error: null});
                }
            } catch (error) {
                if (!controller.signal.aborted) {
                    setAccountData({
                        account: null,
                        error: error instanceof Error ? error.message : "活动额度加载失败",
                    });
                }
            }
        }

        void loadAwards();
        void loadAccount();

        return () => controller.abort();
    }, [activityId]);


    function finishDraw() {
        inFlightRef.current = false;
        stopAnimationRef.current = null;

        if (mountedRef.current) {
            setPhase("idle");
        }
    }

    /**
     * 签到成功后有限次数地刷新活动账户额度。
     *
     * 签到接口只代表返利订单已经创建。活动 SKU 需要经过可靠消息和 MQ
     * 异步结算后才会增加活动账户额度，因此立即查询可能仍得到旧数据。
     *
     * 方法以签到前的总额度作为基准。一旦发现总额度变化便停止轮询；
     * 如果结算尚未完成，则在有限次数后保留最后一次成功查询的数据，
     * 避免浏览器无限请求后端。
     *
     * @returns 额度刷新流程完成后的 Promise
     * @throws Error 所有额度查询均失败时抛出最后一次查询异常
     */
    async function refreshAccountAfterSign(): Promise<void> {
        const previousTotalCount = accountData.account?.totalCount ?? null;
        let lastError: unknown;
        let hasSuccessfulRefresh = false;

        for (const delayMs of ACCOUNT_REFRESH_DELAYS) {
            if (delayMs > 0) {
                await wait(delayMs);
            }

            if (!mountedRef.current) {
                return;
            }

            try {
                const account = await queryActivityAccount(activityId);

                if (!mountedRef.current) {
                    return;
                }

                hasSuccessfulRefresh = true;
                setAccountData({account, error: null});

                if (previousTotalCount === null || account.totalCount !== previousTotalCount) {
                    return;
                }
            } catch (error) {
                lastError = error;
            }
        }

        if (!hasSuccessfulRefresh) {
            setAccountData((current) => ({account: current.account, error: "签到成功，但活动额度暂时无法刷新"}));

            if (lastError instanceof Error) {
                throw lastError;
            }

            throw new Error("签到成功，但活动额度暂时无法刷新");
        }
    }

    /**
     * 保存服务端确认的中奖结果，并重新查询用户视角下的奖品解锁状态。
     *
     * 抽奖结果已经产生后，刷新失败只能作为展示状态异常处理，不能提示用户重新抽奖，
     * 否则可能产生重复订单和重复扣减额度。
     *
     * @param nextOutcome 服务端已经确认的中奖结果
     */
    async function completeDraw(nextOutcome: DrawOutcome): Promise<void> {
        if (!mountedRef.current) {
            inFlightRef.current = false;
            return;
        }

        setOutcome(nextOutcome);
        setPhase("refreshing");

        const [awardsResult, accountResult] = await Promise.allSettled([
            queryActivityAwards(activityId),
            queryActivityAccount(activityId),
        ]);

        if (!mountedRef.current) {
            inFlightRef.current = false;
            return;
        }

        const refreshWarnings: string[] = [];

        if (awardsResult.status === "fulfilled") {
            setPageData({awards: normalizeAwards(awardsResult.value), error: null});
        } else {
            refreshWarnings.push("奖品解锁状态");
        }

        if (accountResult.status === "fulfilled") {
            setAccountData({account: accountResult.value, error: null});
        } else {
            setAccountData((current) => ({account: current.account, error: "抽奖额度刷新失败，请刷新页面核实"}));
            refreshWarnings.push("抽奖额度");
        }

        if (refreshWarnings.length > 0) {
            setDrawError(`抽奖结果已经生成，但${refreshWarnings.join("和")}刷新失败，请刷新页面核实，不要重复抽奖`);
        }

        finishDraw();
    }

    /**
     * 装配活动公共缓存，并在成功后重新读取奖品展示数据。
     *
     * 装配与抽奖互斥，防止概率表和库存缓存重建期间提交抽奖请求。
     */
    async function handleArmory() {
        if (isAssembling || isDrawing) {
            return;
        }

        setArmoryStatus("assembling");
        setArmoryMessage("正在装配活动缓存……");

        try {
            await armoryActivityRaffle(activityId);

            if (!mountedRef.current) {
                return;
            }

            const refreshedAwards = await queryActivityAwards(activityId);

            if (!mountedRef.current) {
                return;
            }

            setPageData({awards: normalizeAwards(refreshedAwards), error: null});
            setArmoryStatus("success");
            setArmoryMessage("活动装配完成，奖品缓存已刷新。");
        } catch (error) {
            if (mountedRef.current) {
                setArmoryStatus("error");
                setArmoryMessage(error instanceof Error ? error.message : "活动装配失败");
            }
        }
    }

    async function handleDraw() {
        // ref 立即生效，防止 React 更新按钮状态前发生连续点击。
        if (inFlightRef.current || isAssembling || isLoading || isAccountLoading || isQuotaExhausted || loadError || awards.length === 0) {
            return;
        }

        inFlightRef.current = true;
        setPhase("requesting");
        setDrawError(null);
        setOutcome(null);
        setActiveIndex(null);

        try {
            const result = await drawActivityRaffle(activityId);

            if (!mountedRef.current) {
                inFlightRef.current = false;
                return;
            }

            if (result.activityId !== activityId) {
                throw new Error("返回的抽奖活动不匹配，请核实本次结果，不要重复提交");
            }

            const targetIndex = awards.findIndex(
                (award) => award.awardId === result.awardId,
            );

            if (targetIndex < 0) {
                setOutcome({
                    result,
                    awardTitle: `奖品编号 ${result.awardId}`,
                });

                setDrawError(
                    "后端已返回抽奖结果，但当前列表中没有对应奖品，请核实奖品配置",
                );

                finishDraw();

                return;
            }

            const nextOutcome: DrawOutcome = {
                result,
                awardTitle: awards[targetIndex].awardTitle,
            }

            if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
                setActiveIndex(targetIndex);
                void completeDraw(nextOutcome);
                return;
            }

            setPhase("animating");

            stopAnimationRef.current = playAnimation(
                awards.length,
                targetIndex,
                setActiveIndex,
                () => {
                    void completeDraw(nextOutcome);
                },
            );
        } catch (error) {
            if (mountedRef.current) {
                setDrawError(
                    error instanceof Error ? error.message : "抽奖请求失败",
                );
            }

            finishDraw();
        }
    }

    const buttonTextByPhase: Record<Phase, string> = {
        idle: "开始抽奖",
        requesting: "正在获取抽奖结果……",
        animating: "正在揭晓……",
        refreshing: "正在刷新活动状态……",
    };
    const buttonText = isQuotaExhausted ? "抽奖次数已用完" : buttonTextByPhase[phase];

    return (
        <main className={styles.page}>
            <section className={styles.container}>
                <header className={styles.header}>
                    <span className={styles.kicker}>MARKET FORGE</span>
                    <h1>幸运抽奖</h1>
                    <p>奖品来自真实策略配置，抽奖结果由服务端决定。</p>
                    <div className={styles.headerMeta}>
                        <small>当前活动：{activityId}</small>
                        <Link href="/slot-machine">老虎机模式</Link>
                    </div>
                </header>

                <ActivityAccountPanel account={accountData.account} error={accountData.error}/>
                <CalendarSignPanel onSigned={refreshAccountAfterSign}/>

                <section
                    className={styles.card}
                    aria-labelledby="award-list-title"
                    aria-busy={isLoading || isDrawing}
                >
                    <div className={styles.cardHeading}>
                        <h2 id="award-list-title">本期奖品</h2>
                        {!isLoading && !loadError && (
                            <span>{awards.length} 个奖品</span>
                        )}
                    </div>

                    {isLoading && (
                        <p role="status">正在加载真实奖品列表……</p>
                    )}

                    {loadError && (
                        <div className={styles.error} role="alert">
                            <p>{loadError}</p>
                            <button
                                type="button"
                                className={styles.secondaryButton}
                                onClick={() => window.location.reload()}
                            >
                                重新加载奖品
                            </button>
                        </div>
                    )}

                    {!isLoading && !loadError && awards.length === 0 && (
                        <p>当前活动尚未配置奖品。</p>
                    )}

                    {awards.length > 0 && (
                        <ul className={styles.grid}>
                            {awards.map((award, index) => (
                                <li
                                    key={award.awardId}
                                    className={[
                                        styles.prize,
                                        activeIndex === index ? styles.active : "",
                                        !award.isAwardUnlock ? styles.locked : "",
                                        award.isAwardUnlock && !award.isAwardWeightUnlock ? styles.weightPending : "",
                                    ]
                                        .filter(Boolean)
                                        .join(" ")}
                                    aria-label={`${award.awardTitle}，${describeUnlockState(award)}`}
                                >
                                    <span className={styles.prizeIcon} aria-hidden="true">奖</span>
                                    <strong>{award.awardTitle}</strong>
                                    <small>{describeUnlockState(award)}</small>
                                </li>
                            ))}
                        </ul>
                    )}

                    <div className={styles.actionGroup}>
                        <button
                            type="button"
                            className={styles.armoryButton}
                            disabled={isAssembling || isDrawing}
                            onClick={() => void handleArmory()}
                        >
                            {isAssembling ? "正在装配……" : "装配活动"}
                        </button>

                        <button
                            type="button"
                            className={styles.drawButton}
                            disabled={isLoading || isAccountLoading || isQuotaExhausted || Boolean(loadError) || awards.length === 0 || isDrawing || isAssembling}
                            onClick={() => void handleDraw()}
                        >
                            {buttonText}
                        </button>
                    </div>

                    {armoryMessage && (
                        <p
                            className={armoryStatus === "error" ? styles.armoryError : styles.armoryStatus}
                            role={armoryStatus === "error" ? "alert" : "status"}
                        >
                            {armoryMessage}
                        </p>
                    )}

                    <p className={styles.notice}>
                        当前为开发联调页面，抽奖资格和次数限制由后端判断。
                    </p>
                </section>

                <StrategyWeightPanel awards={awards}/>

                {drawError && (
                    <p className={styles.error} role="alert">
                        {drawError}
                    </p>
                )}

                <section
                    className={styles.result}
                    aria-live="polite"
                    aria-atomic="true"
                >
                    {outcome && (
                        <>
                            <h2>本次抽奖结果</h2>
                            <strong>{outcome.awardTitle}</strong>
                            <p>活动订单已生成，中奖结果以服务端记录为准。</p>
                            <small>奖品编号：{outcome.result.awardId}</small>
                        </>
                    )}
                </section>
            </section>
        </main>
    );

}

