import {ActivityAward} from "@/features/raffle/model/activity-raffle-types";

/**
 * 生成人类可读的奖品解锁状态。
 *
 * 次数锁按照活动配置区分当日和累计口径；权重规则始终使用累计抽奖次数。
 * 九宫格和老虎机共用该方法，保证两种展示模式不会出现不同的规则文案。
 *
 * @param award 当前用户视角下的活动奖品
 * @returns 页面展示的解锁状态
 */
export function describeUnlockState(award: ActivityAward): string {
    if (!award.isAwardUnlock) {
        const scope = award.awardUnlockScope === "day" ? "今日" : "累计";
        return `${scope}还差 ${award.waitUnLockCount} 次解锁`;
    }

    if (!award.isAwardWeightUnlock) {
        return `累计还差 ${award.waitAwardWeightCount} 次进入权重奖池`;
    }

    return award.awardSubtitle || "已解锁";
}

/**
 * 校验奖品标识唯一性，并按展示顺序生成新的奖品数组。
 *
 * @param awards Java 服务返回的活动奖品
 * @returns 可直接渲染的有序奖品数组
 * @throws Error 奖品列表包含重复奖品标识时抛出
 */
export function normalizeAwards(awards: ActivityAward[]): ActivityAward[] {
    const uniqueIds = new Set(awards.map((award) => award.awardId));

    if (uniqueIds.size !== awards.length) {
        throw new Error("活动奖品列表存在重复奖品编号");
    }

    return [...awards].sort((left, right) => left.sort - right.sort || left.awardId - right.awardId);
}
