"use client";

import {queryCalendarSignStatus, submitCalendarSign} from "@/features/raffle/api/activity-raffle-api";
import {useEffect, useRef, useState} from "react";
import styles from "./calendar-sign-panel.module.css";

type CalendarSignPanelProps = {
    onSigned: () => Promise<void> | void;
};

type SignPhase = "loading" | "unsigned" | "submitting" | "signed" | "error" | "unknown";

const BUTTON_TEXT_BY_PHASE: Record<SignPhase, string> = {
    loading: "正在查询签到状态……",
    unsigned: "今日签到",
    submitting: "正在签到……",
    signed: "今日已签到",
    error: "重新查询签到状态",
    unknown: "确认签到结果",
};

/**
 * 提供日历签到操作并维护当天签到状态。
 *
 * 提交签到发生异常时不会直接开放重试，而是再次查询服务端状态：
 * 如果服务端已经完成签到，则按成功处理；如果连状态也无法确认，
 * 则进入 unknown 状态，只允许用户查询结果，不能重复提交签到。
 *
 * @param props 签到成功后的页面刷新回调
 * @returns 日历签到操作区域
 */
export default function CalendarSignPanel({onSigned}: CalendarSignPanelProps) {
    const [phase, setPhase] = useState<SignPhase>("loading");
    const [message, setMessage] = useState("正在查询今天的签到状态……");
    const mountedRef = useRef(false);
    const inFlightRef = useRef(false);

    /**
     * 从服务端重新读取今日签到状态。
     *
     * 该方法用于用户主动重试状态查询，因此开始查询时需要立即更新加载提示。
     *
     * @param signal 请求取消信号
     * @returns 状态查询完成后的 Promise
     */
    async function loadStatus(signal?: AbortSignal): Promise<void> {
        setPhase("loading");
        setMessage("正在查询今天的签到状态……");

        try {
            const signed = await queryCalendarSignStatus(signal);

            if (signal?.aborted || !mountedRef.current) {
                return;
            }

            setPhase(signed ? "signed" : "unsigned");
            setMessage(signed ? "今天已经完成签到，返利正在或已经结算。" : "完成签到可以获得积分和活动抽奖次数。");
        } catch (error) {
            if (signal?.aborted || !mountedRef.current) {
                return;
            }

            setPhase("error");
            setMessage(error instanceof Error ? error.message : "签到状态查询失败");
        }
    }

    useEffect(() => {
        mountedRef.current = true;
        const controller = new AbortController();

        void queryCalendarSignStatus(controller.signal)
            .then((signed) => {
                if (controller.signal.aborted || !mountedRef.current) {
                    return;
                }

                setPhase(signed ? "signed" : "unsigned");
                setMessage(signed ? "今天已经完成签到，返利正在或已经结算。" : "完成签到可以获得积分和活动抽奖次数。");
            })
            .catch((error: unknown) => {
                if (controller.signal.aborted || !mountedRef.current) {
                    return;
                }

                setPhase("error");
                setMessage(error instanceof Error ? error.message : "签到状态查询失败");
            });

        return () => {
            mountedRef.current = false;
            controller.abort();
        };
    }, []);

    /**
     * 将组件切换为已签到状态，并通知父页面刷新活动额度。
     *
     * 签到成功与额度刷新是两个不同结果。额度刷新失败不能把已经完成的
     * 签到回退为未签到，否则会诱导用户重复提交。
     *
     * @param successMessage 已确认签到后的提示文案
     * @returns 父页面刷新完成后的 Promise
     */
    async function finishConfirmedSign(successMessage: string): Promise<void> {
        if (!mountedRef.current) {
            return;
        }

        setPhase("signed");
        setMessage(successMessage);

        try {
            await onSigned();

            if (mountedRef.current) {
                setMessage("签到成功，返利已经受理，活动额度正在同步。");
            }
        } catch {
            if (mountedRef.current) {
                setMessage("签到已经成功，但活动额度刷新失败，请稍后刷新页面查看。");
            }
        }
    }

    /**
     * 提交今日签到，并在请求异常时主动查询最终签到状态。
     *
     * @returns 签到和状态确认流程完成后的 Promise
     */
    async function handleSign(): Promise<void> {
        if (phase !== "unsigned" || inFlightRef.current) {
            return;
        }

        inFlightRef.current = true;
        setPhase("submitting");
        setMessage("正在提交今日签到，请不要重复操作……");

        try {
            await submitCalendarSign();
            await finishConfirmedSign("签到成功，正在刷新活动额度……");
        } catch (error) {
            if (!mountedRef.current) {
                return;
            }

            const submitMessage = error instanceof Error ? error.message : "无法确认签到结果";

            try {
                const signed = await queryCalendarSignStatus();

                if (!mountedRef.current) {
                    return;
                }

                if (signed) {
                    await finishConfirmedSign("服务端已确认今天签到成功，正在刷新活动额度……");
                } else {
                    setPhase("unsigned");
                    setMessage(submitMessage);
                }
            } catch {
                if (mountedRef.current) {
                    setPhase("unknown");
                    setMessage("暂时无法确认签到结果，请先查询状态，不要重复签到。");
                }
            }
        } finally {
            inFlightRef.current = false;
        }
    }

    function handleButtonClick(): void {
        if (phase === "unsigned") {
            void handleSign();
            return;
        }

        if (phase === "error" || phase === "unknown") {
            void loadStatus();
        }
    }

    const disabled = phase === "loading" || phase === "submitting" || phase === "signed";

    return (
        <section className={styles.panel} aria-labelledby="calendar-sign-title">
            <div className={styles.content}>
                <div>
                    <h2 id="calendar-sign-title">每日签到</h2>
                    <p className={phase === "error" || phase === "unknown" ? styles.error : styles.message}
                       aria-live="polite">
                        {message}
                    </p>
                </div>

                <button type="button" className={styles.button} disabled={disabled} onClick={handleButtonClick}>
                    {BUTTON_TEXT_BY_PHASE[phase]}
                </button>
            </div>
        </section>
    );
}
