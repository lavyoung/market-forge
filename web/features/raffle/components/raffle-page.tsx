"use client"


import {RaffleResult, StrategyAward} from "@/features/raffle/model/raffle-types";
import {useEffect, useRef, useState} from "react";
import {drawRaffle, getStrategyAwards} from "@/features/raffle/api/raffle-api";
import styles from "./raffle-page.module.css";

type RafflePageProp = {
    strategyId: number;
};

type PageData = {
    awards: StrategyAward[];
    error: string | null
};

type DrawOutcome = {
    result: RaffleResult;
    awardTitle: string;
}

type Phase = "idle" | "requesting" | "animating";

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

export default function RafflePage({strategyId}: RafflePageProp) {

    const [pageDate, setPageData] = useState<PageData | null>(null);
    const [phase, setPhase] = useState<Phase>("idle");
    const [activeIndex, setActiveIndex] = useState<number | null>(null);
    const [outcome, setOutcome] = useState<DrawOutcome | null>(null);
    const [drawError, setDrawError] = useState<string | null>(null);

    const mountedRef = useRef(false);
    const inFlightRef = useRef(false);
    const stopAnimationRef = useRef<(() => void) | null>(null);

    const awards = pageDate?.awards ?? [];
    const isLoading = pageDate === null;
    const loadError = pageDate?.error ?? null;
    const isDrawing = phase !== "idle";


    useEffect(() => {
        mountedRef.current = false;

        return () => {
            mountedRef.current = false;
            stopAnimationRef.current?.();
        };
    }, []);


    useEffect(() => {
            const controller = new AbortController();

            async function load() {
                try {
                    const data = await getStrategyAwards(
                        strategyId,
                        controller.signal
                    );

                    if (data.some((award) => award.strategyId !== strategyId)) {
                        throw new Error("奖品列表包含其他策略的数据");
                    }

                    const uniqueIds = new Set(data.map((award) => award.awardId));

                    if (uniqueIds.size !== data.length) {
                        throw new Error("奖品列表存在重复奖品编号");
                    }

                    const sortedAwards = [...data].sort(
                        (left, right) =>
                            left.sort - right.sort || left.awardId - right.awardId,
                    );

                    if (!controller.signal.aborted) {
                        setPageData({
                            awards: sortedAwards,
                            error: null,
                        });
                    }
                } catch (error) {
                    if (!controller.signal.aborted) {
                        setPageData({
                            awards: [],
                            error: error instanceof Error ? error.message : "奖品列表加载失败",
                        })
                    }
                }
            }

            void load();

            return () => {
                controller.abort();
            };
        }, [strategyId]
    );


    function finishDraw() {
        inFlightRef.current = false;
        stopAnimationRef.current = null;

        if (mountedRef.current) {
            setPhase("idle");
        }
    }

    async function handleDraw() {
        // ref 立即生效，防止 React 更新按钮状态前发生连续点击。
        if (inFlightRef.current || isLoading || loadError || awards.length === 0) {
            return;
        }

        inFlightRef.current = true;
        setPhase("requesting");
        setDrawError(null);
        setOutcome(null);
        setActiveIndex(null);

        try {
            const result = await drawRaffle(strategyId);

            if (!mountedRef.current) {
                inFlightRef.current = false;
                return;
            }

            if (result.strategyId !== strategyId) {
                throw new Error("返回的抽奖策略不匹配，请核实本次结果，不要重复提交");
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
                setOutcome(nextOutcome);
                finishDraw();
                return;
            }

            setPhase("animating");

            stopAnimationRef.current = playAnimation(
                awards.length,
                targetIndex,
                setActiveIndex,
                () => {
                    if (mountedRef.current) {
                        setOutcome(nextOutcome);
                    }

                    finishDraw();
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
                : "开始抽奖";
    return (
        <main className={styles.page}>
            <section className={styles.container}>
                <header className={styles.header}>
                    <span className={styles.kicker}>MARKET FORGE</span>
                    <h1>幸运抽奖</h1>
                    <p>奖品来自真实策略配置，抽奖结果由服务端决定。</p>
                    <small>当前策略：{strategyId}</small>
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
                        <p>当前策略尚未配置奖品。</p>
                    )}

                    {awards.length > 0 && (
                        <ul className={styles.grid}>
                            {awards.map((award, index) => (
                                <li
                                    key={award.awardId}
                                    className={[
                                        styles.prize,
                                        activeIndex === index ? styles.active : "",
                                    ]
                                        .filter(Boolean)
                                        .join(" ")}
                                >
                  <span className={styles.prizeIcon} aria-hidden="true">
                    🎁
                  </span>
                                    <strong>{award.awardTitle}</strong>
                                    <small>奖品编号：{award.awardId}</small>
                                </li>
                            ))}
                        </ul>
                    )}

                    <button
                        type="button"
                        className={styles.drawButton}
                        disabled={
                            isLoading ||
                            Boolean(loadError) ||
                            awards.length === 0 ||
                            isDrawing
                        }
                        onClick={() => void handleDraw()}
                    >
                        {buttonText}
                    </button>

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
                            <p>
                                {outcome.result.awardDesc || "服务端已返回本次抽奖结果。"}
                            </p>
                            <small>奖品编号：{outcome.result.awardId}</small>
                        </>
                    )}
                </section>
            </section>
        </main>
    );

}

