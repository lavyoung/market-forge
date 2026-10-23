import type {ActivityAward} from "@/features/raffle/model/activity-raffle-types";
import {normalizeAwards} from "@/features/raffle/model/raffle-award-view";
import {describe, expect, it} from "vitest";

/**
 * 创建用于奖品排序与去重测试的完整活动奖品。
 *
 * @param awardId 奖品标识
 * @param sort 业务展示顺序
 * @returns 字段完整的活动奖品
 */
function createAward(awardId: string, sort: number): ActivityAward {
    return {
        awardId,
        awardTitle: `奖品 ${awardId}`,
        awardSubtitle: null,
        sort,
        awardRuleLockCount: null,
        awardUnlockScope: "total",
        isAwardUnlock: true,
        waitUnLockCount: 0,
        awardRuleWeightCount: null,
        isAwardWeightUnlock: true,
        waitAwardWeightCount: 0,
    };
}

describe("normalizeAwards", () => {
    it("应优先按业务顺序排序并使用字符串标识稳定同序奖品", () => {
        // Given
        const awards = [
            createAward("9007199254740995", 2),
            createAward("9007199254740994", 1),
            createAward("9007199254740993", 2),
        ];

        // When
        const normalized = normalizeAwards(awards);

        // Then
        expect(normalized.map((award) => award.awardId)).toEqual([
            "9007199254740994",
            "9007199254740993",
            "9007199254740995",
        ]);
        expect(awards.map((award) => award.awardId)).toEqual([
            "9007199254740995",
            "9007199254740994",
            "9007199254740993",
        ]);
    });

    it("应拒绝重复的超大字符串奖品标识", () => {
        // Given
        const awards = [
            createAward("9007199254740993", 1),
            createAward("9007199254740993", 2),
        ];

        // When / Then
        expect(() => normalizeAwards(awards)).toThrow("活动奖品列表存在重复奖品编号");
    });
});
