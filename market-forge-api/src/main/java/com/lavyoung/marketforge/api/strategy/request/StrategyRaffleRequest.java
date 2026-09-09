package com.lavyoung.marketforge.api.strategy.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;

/**
 * 执行一次策略抽奖的请求契约。
 *
 * @param userId     参与抽奖的用户标识
 * @param strategyId 抽奖策略标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/09
 */
public record StrategyRaffleRequest(
        @NotBlank(message = "用户标识不能为空") String userId,
        @NotNull(message = "策略标识不能为空")
        @Positive(message = "策略标识必须大于零") Long strategyId
) implements Serializable {
}
