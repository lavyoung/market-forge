package com.lavyoung.marketforge.api.strategy.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

/**
 * 策略概率快照刷新请求的受理结果。
 *
 * <p>该响应只说明刷新请求已经进入应用层调度流程，不代表策略概率快照已经
 * 完成重建。调用方不得根据该响应立即推断 Redis 中已经切换到新版本。</p>
 *
 * @param strategyId       请求刷新的策略标识
 * @param newTaskScheduled 是否创建了新的异步任务；为 {@code false} 表示请求已合并到现有任务
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@Schema(description = "策略概率快照刷新请求的受理结果")
public record StrategySnapshotRefreshResponse(
        @Schema(description = "策略标识", example = "100001")
        Long strategyId,
        @Schema(description = "是否为该策略创建了新的异步任务", example = "true")
        boolean newTaskScheduled
) {

    /**
     * 校验成功响应必须包含有效的策略标识。
     *
     * @throws NullPointerException     策略标识为空时抛出
     * @throws IllegalArgumentException 策略标识不是正数时抛出
     */
    public StrategySnapshotRefreshResponse {
        Objects.requireNonNull(strategyId, "strategyId must not be null");
        if (strategyId <= 0) {
            throw new IllegalArgumentException("strategyId must be positive");
        }
    }
}
