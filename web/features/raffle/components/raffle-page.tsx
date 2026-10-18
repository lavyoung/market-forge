"use client"


import {ActivityAward, ActivityDrawResult} from "@/features/raffle/model/activity-raffle-types";
import {
    armoryActivityRaffle,
    drawActivityRaffle,
    queryActivityAwards,
} from "@/features/raffle/api/activity-raffle-api";
import {describeUnlockState, normalizeAwards,} from "@/features/raffle/model/raffle-award-view";
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

type DrawOutcome = {
    result: ActivityDrawResult;
    awardTitle: string;
};

type Phase = "idle" | "requesting" | "animating" | "refreshing";
type ArmoryStatus = "idle" | "assembling" | "success" | "error";

/**
 * 播放展示动画，最终停在后端指定的奖品位置。
 *
 * 动画不决定中奖结果，也不请求后端。
 * 返回清理函数，用于页面退出时停止动画。
 */
function playAnimation(count: number,
                       targetIndex: number,
                       onStep: (index: number) => void,
                       onComplete: () => void): () => void {
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

    const [pageDate, setPageData] = useState<PageData | null>(null);
    const [phase, setPhase] = useState<Phase>("idle");
    const [activeIndex, setActiveIndex] = useState<number | null>(null);
    const [outcome, setOutcome] = useState<DrawOutcome | null>(null);
    const [drawError, setDrawError] = useState<string | null>(null);
    const [armoryStatus, setArmoryStatus] = useState<ArmoryStatus>("idle");
    const [armoryMessage, setArmoryMessage] = useState<string | null>(null);

    const mountedRef = useRef(false);
    const inFlightRef = useRef(false);
    const stopAnimationRef = useRef<(() => void) | null>(null);

    const awards = pageDate?.awards ?? [];
    const isLoading = pageDate === null;
    const loadError = pageDate?.error ?? null;
    const isDrawing = phase !== "idle";
    const isAssembling = armoryStatus === "assembling";


    useEffect(() => {
        mountedRef.current = true;

        return () => {
            mountedRef.current = false;
            stopAnimationRef.current?.();
        };
    }, []);


    useEffect(() => {
        const controller = new AbortController();

        async function load() {
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

        void load();

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
     * 保存服务端确认的中奖结果，并重新查询用户视角下的奖品解锁状态。
     *
     * 抽奖结果已经产生后，刷新失败只能作为展示状态异常处理，不能提示用户重新抽奖，
     * 否则可能产生重复订单和重复扣减额度。
     *
     * @param nextOutcome 服务端已经确认的中奖结果
     */
    async function completeDraw(nextOutcome: DrawOutcome) {
        if (!mountedRef.current) {
            inFlightRef.current = false;
            return;
        }

        setOutcome(nextOutcome);
        setPhase("refreshing");

        try {
            const refreshedAwards = await queryActivityAwards(activityId);

            if (mountedRef.current) {
                setPageData({awards: normalizeAwards(refreshedAwards), error: null});
            }
        } catch {
            if (mountedRef.current) {
                setDrawError("抽奖结果已经生成，但奖品解锁状态刷新失败，请刷新页面核实，不要重复抽奖");
            }
        } finally {
            finishDraw();
        }
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
        if (inFlightRef.current || isAssembling || isLoading || loadError || awards.length === 0) {
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

    const buttonText =
        phase === "requesting"
            ? "正在获取抽奖结果……"
            : phase === "animating"
                ? "正在揭晓……"
                : phase === "refreshing"
                    ? "正在刷新奖品状态……"
                : "开始抽奖";
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
                            disabled={
                                isLoading ||
                                Boolean(loadError) ||
                                awards.length === 0 ||
                                isDrawing ||
                                isAssembling
                            }
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

