import RafflePage from "@/features/raffle/components/raffle-page";

/**
 * 读取当前部署环境允许访问的活动标识。
 *
 * @returns 经过安全整数校验的活动标识
 * @throws Error 活动配置缺失或非法时抛出
 */
function resolveActivityId(): number {
    const activityId = Number(process.env.DEMO_RAFFLE_ACTIVITY_ID);

    if (!Number.isSafeInteger(activityId) || activityId <= 0) {
        throw new Error("DEMO_RAFFLE_ACTIVITY_ID 必须配置为安全范围内的正整数");
    }

    return activityId;
}

export default function Page() {
    const activityId = resolveActivityId();
    return <RafflePage key={activityId} activityId={activityId}/>;
}
