package com.lavyoung.marketforge.application.strategy.model;

import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 执行抽奖用例的命令。
 *
 * <p>活动抽奖入口会携带活动标识、奖品解锁统计范围、
 * 业务日期和活动结束时间。直接按策略抽奖的兼容入口
 * 可以不携带活动上下文。</p>
 *
 * @param userId           用户标识
 * @param strategyId       抽奖策略标识
 * @param activityId       活动标识；非活动抽奖时可为空
 * @param awardUnlockScope 奖品解锁次数统计范围；非活动抽奖时可为空
 * @param statisticsDate   解锁次数统计所使用的业务日期；非活动抽奖时可为空
 * @param endDateTime      活动结束时间；非活动抽奖时可为空
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/17
 */
public record RaffleCommand(
        String userId,
        Long strategyId,
        Long activityId,
        AwardUnlockScopeVO awardUnlockScope,
        LocalDate statisticsDate,
        LocalDateTime endDateTime
) {

    /**
     * 创建不带活动上下文的兼容抽奖命令。
     *
     * @param userId     用户标识
     * @param strategyId 抽奖策略标识
     */
    public RaffleCommand(String userId, Long strategyId) {
        this(userId, strategyId, null, null, null, null);
    }

    /**
     * 创建只携带活动结束时间的兼容抽奖命令。
     *
     * @param userId      用户标识
     * @param strategyId  抽奖策略标识
     * @param endDateTime 活动结束时间
     */
    public RaffleCommand(String userId,
                         Long strategyId,
                         LocalDateTime endDateTime) {
        this(userId, strategyId, null, null, null, endDateTime);
    }

    /**
     * 校验抽奖命令和活动上下文的完整性。
     *
     * @throws IllegalArgumentException 用户标识为空白、标识非正数，
     *                                  或活动上下文字段不完整时抛出
     * @throws NullPointerException     策略标识为空时抛出
     */
    public RaffleCommand {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }

        Objects.requireNonNull(strategyId, "strategyId must not be null");
        if (strategyId <= 0) {
            throw new IllegalArgumentException("strategyId must be positive");
        }

        if (activityId != null && activityId <= 0) {
            throw new IllegalArgumentException("activityId must be positive");
        }

        boolean containsActivityContext = activityId != null || awardUnlockScope != null || statisticsDate != null;

        boolean hasCompleteActivityContext = activityId != null && awardUnlockScope != null && statisticsDate != null;

        if (containsActivityContext && !hasCompleteActivityContext) {
            throw new IllegalArgumentException("activity unlock context must be complete");
        }
    }

    /**
     * 判断当前命令是否携带完整活动上下文。
     *
     * @return 携带活动标识、解锁范围和统计日期时返回 {@code true}
     */
    public boolean hasActivityContext() {
        return activityId != null;
    }
}
