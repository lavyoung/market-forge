"use client"

import {drawActivityRaffle, queryActivityAwards,} from "@/features/raffle/api/activity-raffle-api";
import {ActivityAward, ActivityDrawResult} from "@/features/raffle/model/activity-raffle-types";
import {describeUnlockState, normalizeAwards,} from "@/features/raffle/model/raffle-award-view";
import {SlotMachine, SlotMachinePrize, SlotMachineSlot} from "@lucky-canvas/react";
import Link from "next/link";
import {useEffect, useMemo, useRef, useState, useSyncExternalStore} from "react";
import styles from "./slot-machine-page.module.css";

type SlotMachinePageProps = {
    activityId: string;
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

const SLOT_COUNT = 3;
const REDUCED_MOTION_QUERY = "(prefers-reduced-motion: reduce)";

const SLOT_BLOCKS = [
    {background: "#202533", borderRadius: "8px", padding: "12px"},
];

const NORMAL_CONFIG = {
    mode: "vertical" as const,
    rowSpacing: 10,
    colSpacing: 10,
    accelerationTime: 700,
    decelerationTime: 1300,
    speed: 22,
};

const REDUCED_MOTION_CONFIG = {
    ...NORMAL_CONFIG,
    accelerationTime: 80,
    decelerationTime: 120,
};

function subscribeReducedMotion(onStoreChange: () => void): () => void {
    const mediaQuery = window.matchMedia(REDUCED_MOTION_QUERY);
    mediaQuery.addEventListener("change", onStoreChange);
    return () => mediaQuery.removeEventListener("change", onStoreChange);
}

function getReducedMotionSnapshot(): boolean {
    return window.matchMedia(REDUCED_MOTION_QUERY).matches;
}

function getReducedMotionServerSnapshot(): boolean {
    return false;
}

function subscribeViewport(onStoreChange: () => void): () => void {
    window.addEventListener("resize", onStoreChange);
    return () => window.removeEventListener("resize", onStoreChange);
}

function getMachineWidthSnapshot(): number {
    return Math.min(480, Math.max(220, window.innerWidth - 72));
}

function getMachineWidthServerSnapshot(): number {
    return 480;
}

/**
 * 基于真实活动奖品渲染三滚轴老虎机，并将后端抽奖结果映射为滚轴落点。
 *
 * 动画组件只负责展示。中奖奖品、订单创建、次数扣减与发奖均由 Java 服务决定，
 * 前端不会随机生成结果，也不会在请求失败时自动重试。
 *
 * @param props 页面活动配置
 * @returns 可执行真实活动抽奖的老虎机页面
 */
export default function SlotMachinePage({activityId}: SlotMachinePageProps) {
    const [pageData, setPageData] = useState<PageData | null>(null);
    const [phase, setPhase] = useState<Phase>("idle");
    const [outcome, setOutcome] = useState<DrawOutcome | null>(null);
    const [drawError, setDrawError] = useState<string | null>(null);
    const reducedMotion = useSyncExternalStore(
        subscribeReducedMotion,
        getReducedMotionSnapshot,
        getReducedMotionServerSnapshot,
    );
    const machineWidth = useSyncExternalStore(
        subscribeViewport,
        getMachineWidthSnapshot,
        getMachineWidthServerSnapshot,
    );
    const machineHeight = Math.round(machineWidth * 11 / 24);

    const slotRef = useRef<SlotMachine | null>(null);
    const mountedRef = useRef(false);
    const inFlightRef = useRef(false);
    const pendingOutcomeRef = useRef<DrawOutcome | null>(null);

    const awards = useMemo(() => pageData?.awards ?? [], [pageData]);
    const isLoading = pageData === null;
    const loadError = pageData?.error ?? null;
    const isDrawing = phase !== "idle";

    const slotPrizes = useMemo<SlotMachinePrize[]>(() => awards.map((award, index) => ({
        background: index % 2 === 0 ? "#fff4ca" : "#e8f3ee",
        borderRadius: "6px",
        fonts: [{
            text: award.awardTitle,
            top: "34%",
            fontColor: "#202533",
            fontSize: 14,
            fontWeight: 700,
            lineHeight: 18,
            lengthLimit: "82%",
            lineClamp: 2,
        }],
    })), [awards]);

    const slots = useMemo<SlotMachineSlot[]>(() => {
        const order = awards.map((_, index) => index);

        return [
            {order, speed: 20, direction: 1},
            {order: [...order].reverse(), speed: 24, direction: -1},
            {order, speed: 28, direction: 1},
        ];
    }, [awards]);

    useEffect(() => {
        mountedRef.current = true;
        const machineRef = slotRef;

        return () => {
            mountedRef.current = false;
            machineRef.current?.stop(new Array(SLOT_COUNT).fill(-1));
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

        void loadAwards();
        return () => controller.abort();
    }, [activityId]);

    function finishDraw() {
        inFlightRef.current = false;

        if (mountedRef.current) {
            setPhase("idle");
        }
    }

    /**
     * 保存服务端中奖结果，并刷新抽奖后发生变化的次数解锁状态。
     *
     * @param nextOutcome 服务端已经确认并落单的中奖结果
     * @returns 奖品状态刷新完成后的 Promise
     */
    async function completeDraw(nextOutcome: DrawOutcome): Promise<void> {
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
     * 提交一次真实活动抽奖，并用三个相同的目标索引停止滚轴。
     *
     * @returns 抽奖请求提交完成后的 Promise；滚轴结束与状态刷新由回调继续处理
     */
    async function handleDraw(): Promise<void> {
        if (inFlightRef.current || isLoading || loadError || awards.length === 0 || !slotRef.current) {
            return;
        }

        inFlightRef.current = true;
        pendingOutcomeRef.current = null;
        setPhase("requesting");
        setOutcome(null);
        setDrawError(null);
        slotRef.current.play();

        try {
            const result = await drawActivityRaffle(activityId);

            if (!mountedRef.current) {
                inFlightRef.current = false;
                return;
            }

            if (result.activityId !== activityId) {
                throw new Error("返回的抽奖活动不匹配，请核实本次结果，不要重复提交");
            }

            const targetIndex = awards.findIndex((award) => award.awardId === result.awardId);
            const nextOutcome = {
                result,
                awardTitle: result.awardTitle,
            };

            if (targetIndex < 0) {
                slotRef.current?.stop(new Array(SLOT_COUNT).fill(-1));
                setDrawError("后端已返回抽奖结果，但当前列表中没有对应奖品，请核实奖品配置");
                await completeDraw(nextOutcome);
                return;
            }

            pendingOutcomeRef.current = nextOutcome;
            setPhase("animating");
            slotRef.current.stop(new Array(SLOT_COUNT).fill(targetIndex));
        } catch (error) {
            slotRef.current?.stop(new Array(SLOT_COUNT).fill(-1));

            if (mountedRef.current) {
                setDrawError(error instanceof Error ? error.message : "抽奖请求失败");
            }

            finishDraw();
        }
    }

    /**
     * 在老虎机三个滚轴停止后提交页面结果，并继续刷新解锁状态。
     */
    function handleAnimationEnd() {
        const pendingOutcome = pendingOutcomeRef.current;
        pendingOutcomeRef.current = null;

        if (!pendingOutcome) {
            finishDraw();
            return;
        }

        void completeDraw(pendingOutcome);
    }

    const buttonText = phase === "requesting"
        ? "正在确认中奖结果……"
        : phase === "animating"
            ? "滚轴正在停止……"
            : phase === "refreshing"
                ? "正在刷新奖品状态……"
                : "启动老虎机";

    return (
        <main className={styles.page}>
            <header className={styles.header}>
                <div>
                    <span className={styles.kicker}>MARKET FORGE LAB</span>
                    <h1>老虎机抽奖</h1>
                    <p>三轴动画展示同一个服务端中奖结果，滚轴不会自行决定奖品。</p>
                </div>
                <Link href="/">返回九宫格</Link>
            </header>

            <section className={styles.machineBand} aria-labelledby="slot-machine-title">
                <div className={styles.machineCopy}>
                    <small>当前活动 {activityId}</small>
                    <h2 id="slot-machine-title">三轴同时命中才是本次结果</h2>
                    <p>抽奖请求只提交一次。服务端返回奖品后，三个滚轴才会减速并停到对应位置。</p>

                    <button
                        type="button"
                        className={styles.drawButton}
                        disabled={isLoading || Boolean(loadError) || awards.length === 0 || isDrawing}
                        onClick={() => void handleDraw()}
                    >
                        {buttonText}
                    </button>

                    {drawError && <p className={styles.error} role="alert">{drawError}</p>}
                </div>

                <div className={styles.machineFrame} aria-busy={isLoading || isDrawing}>
                    {isLoading && <p role="status">正在加载真实奖品列表……</p>}

                    {loadError && (
                        <div className={styles.error} role="alert">
                            <p>{loadError}</p>
                            <button type="button" onClick={() => window.location.reload()}>重新加载</button>
                        </div>
                    )}

                    {!isLoading && !loadError && awards.length === 0 && <p>当前活动尚未配置奖品。</p>}

                    {awards.length > 0 && (
                        <SlotMachine
                            ref={slotRef}
                            width={machineWidth}
                            height={machineHeight}
                            blocks={SLOT_BLOCKS}
                            prizes={slotPrizes}
                            slots={slots}
                            defaultConfig={reducedMotion ? REDUCED_MOTION_CONFIG : NORMAL_CONFIG}
                            defaultStyle={{
                                background: "#fff4ca",
                                borderRadius: "6px",
                                fontColor: "#202533",
                                fontSize: 14,
                                fontWeight: 700,
                                lineHeight: 18,
                                lengthLimit: "82%",
                                lineClamp: 2,
                            }}
                            onEnd={handleAnimationEnd}
                        />
                    )}
                </div>
            </section>

            <section className={styles.statusBand}>
                <div className={styles.result} aria-live="polite" aria-atomic="true">
                    <span>本次结果</span>
                    {outcome ? (
                        <>
                            <strong>{outcome.awardTitle}</strong>
                            <small>订单 {outcome.result.orderId}</small>
                        </>
                    ) : (
                        <p>启动后将在这里展示服务端确认的奖品。</p>
                    )}
                </div>

                <div className={styles.awardStatus}>
                    <span>奖品状态</span>
                    <ul>
                        {awards.map((award) => (
                            <li key={award.awardId}>
                                <strong>{award.awardTitle}</strong>
                                <small>{describeUnlockState(award)}</small>
                            </li>
                        ))}
                    </ul>
                </div>
            </section>
        </main>
    );
}
