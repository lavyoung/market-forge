package com.lavyoung.marketforge.domain.award.model.vo;

/**
 * 奖品解锁判定结果。
 *
 * <p>该值对象同时承载页面展示和抽奖规则校验所需的数据，
 * 防止不同调用方分别计算解锁状态而产生规则不一致。</p>
 *
 * @param requiredCount  奖品要求完成的抽奖次数
 * @param completedCount 用户在指定统计范围内已经完成的抽奖次数
 * @param unlocked       是否已经满足奖品解锁条件
 * @param remainingCount 距离解锁还需要完成的抽奖次数；已解锁时固定为零
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/17
 */
public record AwardUnlockResultVO(
        int requiredCount,
        long completedCount,
        boolean unlocked,
        int remainingCount
) {

    /**
     * 校验奖品解锁结果内部状态的一致性。
     *
     * @throws IllegalArgumentException 次数为负数，或者解锁状态与次数计算结果不一致时抛出
     */
    public AwardUnlockResultVO {
        if (requiredCount < 0) {
            throw new IllegalArgumentException("requiredCount must not be negative");
        }
        if (completedCount < 0) {
            throw new IllegalArgumentException("completedCount must not be negative");
        }
        if (remainingCount < 0) {
            throw new IllegalArgumentException("remainingCount must not be negative");
        }

        boolean expectedUnlocked = completedCount >= requiredCount;
        if (unlocked != expectedUnlocked) {
            throw new IllegalArgumentException(
                    "unlocked must be consistent with completedCount and requiredCount");
        }

        long expectedRemainingCount = Math.max(
                0L,
                (long) requiredCount - completedCount
        );
        if (remainingCount != Math.toIntExact(expectedRemainingCount)) {
            throw new IllegalArgumentException(
                    "remainingCount must be consistent with completedCount and requiredCount");
        }
    }

    /**
     * 根据要求次数和已完成次数生成解锁判定结果。
     *
     * <p>要求次数为零时，表示奖品没有次数门槛，应立即视为已解锁。</p>
     *
     * @param requiredCount  奖品要求完成的抽奖次数
     * @param completedCount 用户已经完成的抽奖次数
     * @return 内部状态一致的奖品解锁判定结果
     * @throws IllegalArgumentException 任意次数为负数时抛出
     * @throws ArithmeticException      剩余次数无法转换为整数时抛出
     */
    public static AwardUnlockResultVO evaluate(int requiredCount, long completedCount) {
        if (requiredCount < 0) {
            throw new IllegalArgumentException("requiredCount must not be negative");
        }
        if (completedCount < 0) {
            throw new IllegalArgumentException("completedCount must not be negative");
        }
        boolean unlocked = completedCount >= requiredCount;
        int remainingCount = unlocked ? 0 : Math.toIntExact((long) requiredCount - completedCount);
        return new AwardUnlockResultVO(
                requiredCount,
                completedCount,
                unlocked,
                remainingCount
        );
    }
}
