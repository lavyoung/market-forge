package com.lavyoung.marketforge.application.strategy.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 执行抽奖用例的命令。
 * <p>
 * 活动抽奖入口会携带活动结束时间，用于策略库存扣减时计算 Redis 锁租约。
 * 直接按策略抽奖的旧入口可以不传该字段，由下游使用短租约兼容。
 *
 * @param userId      用户标识
 * @param strategyId  抽奖策略标识
 * @param endDateTime 活动结束时间；非活动维度抽奖时可为空
 */
public record RaffleCommand(
        String userId,
        Long strategyId,
        LocalDateTime endDateTime
) {

    /**
     * 创建不带活动结束时间的抽奖命令。
     * <p>
     * 该构造器用于兼容按策略直接抽奖的旧调用；库存锁会使用默认短租约。
     *
     * @param userId     用户标识
     * @param strategyId 抽奖策略标识
     */
    public RaffleCommand(String userId, Long strategyId) {
        this(userId, strategyId, null);
    }

    /**
     * 校验应用层用例必须满足的基本前置条件。
     *
     * @throws IllegalArgumentException 用户标识为空白或策略标识非正数时抛出
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
    }
}
