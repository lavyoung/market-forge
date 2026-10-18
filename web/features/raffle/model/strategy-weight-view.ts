import type {ActivityAward} from "@/features/raffle/model/activity-raffle-types";

/**
 * 权重门槛下新增进入奖池的奖品。
 */
export type StrategyWeightAward = {
    awardId: number;
    awardTitle: string;
};

/**
 * 前端展示使用的权重奖池门槛。
 *
 * threshold 表示累计抽奖次数门槛，awards 表示达到该门槛时
 * 首次进入权重奖池的奖品，不代表该档位完整奖池。
 */
export type StrategyWeightTier = {
    threshold: number;
    unlocked: boolean;
    remainingCount: number;
    awards: StrategyWeightAward[];
};

/**
 * 将奖品级权重字段整理为按最低门槛分组的展示模型。
 *
 * 没有配置权重门槛的普通奖品不会出现在结果中。
 * 同一门槛下的奖品按照原始展示顺序排列，门槛按照从低到高排列。
 *
 * @param awards 当前用户视角下的活动奖品
 * @returns 按最低进入门槛排序的权重奖池列表
 */
export function buildStrategyWeightTiers(awards: ActivityAward[]): StrategyWeightTier[] {
    const awardGroups = new Map<number, ActivityAward[]>();

    for (const award of awards) {
        if (award.awardRuleWeightCount === null) {
            continue;
        }

        const tierAwards = awardGroups.get(award.awardRuleWeightCount) ?? [];
        tierAwards.push(award);
        awardGroups.set(award.awardRuleWeightCount, tierAwards);
    }

    return Array.from(awardGroups.entries())
        .sort(([leftThreshold], [rightThreshold]) => leftThreshold - rightThreshold)
        .map(([threshold, tierAwards]) => {
            const sortedAwards = [...tierAwards].sort((left, right) => left.sort - right.sort);

            return {
                threshold,
                unlocked: sortedAwards.every((award) => award.isAwardWeightUnlock),
                remainingCount: Math.max(...sortedAwards.map((award) => award.waitAwardWeightCount)),
                awards: sortedAwards.map((award) => ({
                    awardId: award.awardId,
                    awardTitle: award.awardTitle,
                })),
            };
        });
}