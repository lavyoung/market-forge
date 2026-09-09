package com.lavyoung.marketforge.application.strategy.model;

import java.util.Objects;

/**
 * 执行抽奖用例的命令。
 *
 * @param userId     用户标识
 * @param strategyId 抽奖策略标识
 */
public record RaffleCommand(String userId, Long strategyId) {

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
