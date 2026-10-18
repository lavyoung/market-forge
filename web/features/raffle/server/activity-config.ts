/**
 * 读取当前部署环境允许访问的活动标识。
 *
 * 活动标识只从服务端环境变量读取，浏览器不能通过查询参数切换到任意活动，
 * 从而与 BFF 对抽奖和装配请求的活动校验保持一致。
 *
 * @returns 经过安全整数校验的活动标识
 * @throws Error 活动配置缺失或非法时抛出
 */
export function resolveActivityId(): number {
    const activityId = Number(process.env.DEMO_RAFFLE_ACTIVITY_ID);

    if (!Number.isSafeInteger(activityId) || activityId <= 0) {
        throw new Error("DEMO_RAFFLE_ACTIVITY_ID 必须配置为安全范围内的正整数");
    }

    return activityId;
}
