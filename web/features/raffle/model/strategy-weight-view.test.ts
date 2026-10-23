import type {ActivityAward} from "@/features/raffle/model/activity-raffle-types";
import {buildStrategyWeightTiers} from "@/features/raffle/model/strategy-weight-view";
import {describe, expect, it} from "vitest";

/**
 * 创建具备默认值的活动奖品测试数据。
 *
 * @param overrides 当前测试场景需要覆盖的字段
 * @returns 完整的活动奖品
 */
function createAward(overrides: Partial<ActivityAward> = {}): ActivityAward {
    return {
        awardId: "1",
        awardTitle: "测试奖品",
        awardSubtitle: null,
        sort: 1,
        awardRuleLockCount: null,
        awardUnlockScope: "total",
        isAwardUnlock: true,
        waitUnLockCount: 0,
        awardRuleWeightCount: null,
        isAwardWeightUnlock: true,
        waitAwardWeightCount: 0,
        ...overrides,
    };
}

describe("buildStrategyWeightTiers", () => {
    it("应按权重门槛分组排序并排除普通奖品", () => {
        // Given
        const awards = [
            createAward({
                awardId: "5",
                awardTitle: "高门槛奖品",
                sort: 5,
                awardRuleWeightCount: 60_000,
                isAwardWeightUnlock: false,
                waitAwardWeightCount: 59_990
            }),
            createAward({awardId: "2", awardTitle: "普通奖品", sort: 2}),
            createAward({
                awardId: "4",
                awardTitle: "低门槛奖品 B",
                sort: 4,
                awardRuleWeightCount: 40_000,
                isAwardWeightUnlock: false,
                waitAwardWeightCount: 39_990
            }),
            createAward({
                awardId: "3",
                awardTitle: "低门槛奖品 A",
                sort: 3,
                awardRuleWeightCount: 40_000,
                isAwardWeightUnlock: false,
                waitAwardWeightCount: 39_990
            }),
        ];

        // When
        const tiers = buildStrategyWeightTiers(awards);

        // Then
        expect(tiers).toEqual([
            {
                threshold: 40_000,
                unlocked: false,
                remainingCount: 39_990,
                awards: [
                    {awardId: "3", awardTitle: "低门槛奖品 A"},
                    {awardId: "4", awardTitle: "低门槛奖品 B"},
                ],
            },
            {
                threshold: 60_000,
                unlocked: false,
                remainingCount: 59_990,
                awards: [{awardId: "5", awardTitle: "高门槛奖品"}],
            },
        ]);
    });

    it("同一门槛状态不一致时应采用保守展示结果", () => {
        // Given
        const awards = [
            createAward({awardId: "1", awardRuleWeightCount: 100, isAwardWeightUnlock: true, waitAwardWeightCount: 0}),
            createAward({
                awardId: "2",
                awardRuleWeightCount: 100,
                isAwardWeightUnlock: false,
                waitAwardWeightCount: 20
            }),
        ];

        // When
        const [tier] = buildStrategyWeightTiers(awards);

        // Then
        expect(tier.unlocked).toBe(false);
        expect(tier.remainingCount).toBe(20);
    });

    it("整理展示顺序时不应修改原奖品数组", () => {
        // Given
        const awards = [
            createAward({awardId: "2", sort: 2, awardRuleWeightCount: 100}),
            createAward({awardId: "1", sort: 1, awardRuleWeightCount: 100}),
        ];

        // When
        buildStrategyWeightTiers(awards);

        // Then
        expect(awards.map((award) => award.awardId)).toEqual(["2", "1"]);
    });
});
