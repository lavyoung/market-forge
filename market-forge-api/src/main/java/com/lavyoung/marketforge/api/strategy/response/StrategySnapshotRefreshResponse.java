package com.lavyoung.marketforge.api.strategy.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 策略概率快照刷新请求的受理结果。
 *
 * <p>该响应只说明刷新请求已经进入应用层调度流程，不代表策略概率快照已经完成重建。
 * 调用方不得根据该响应立即推断 Redis 中已经切换到新版本。</p>
 *
 * <p>策略标识通过字符串返回，避免标识超过 JavaScript 安全整数范围后发生精度丢失。
 * 服务端内部仍然使用 {@link Long} 完成参数校验和业务调用。</p>
 *
 * @param strategyId       请求刷新的策略标识
 * @param newTaskScheduled 是否创建了新的异步任务；为 {@code false} 表示请求已合并到现有任务
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@Schema(description = "策略概率快照刷新请求的受理结果")
public record StrategySnapshotRefreshResponse(
        @Schema(description = "策略标识", example = "9007199254740993", type = "string")
        String strategyId,

        @Schema(description = "是否为该策略创建了新的异步任务", example = "true")
        boolean newTaskScheduled
) {

    private static final Pattern POSITIVE_IDENTIFIER_PATTERN = Pattern.compile("[1-9][0-9]*");

    /**
     * 校验成功响应必须包含有效的十进制正整数策略标识。
     *
     * <p>虽然正常响应由 Controller 根据已经校验的 {@link Long} 创建，但 API 模型仍然
     * 在自身边界保护不变量，避免其他调用方构造空字符串、负数或非数字标识。</p>
     *
     * @throws NullPointerException     策略标识为空时抛出
     * @throws IllegalArgumentException 策略标识不是十进制正整数时抛出
     */
    public StrategySnapshotRefreshResponse {
        Objects.requireNonNull(strategyId, "strategyId must not be null");
        if (!POSITIVE_IDENTIFIER_PATTERN.matcher(strategyId).matches()) {
            throw new IllegalArgumentException("strategyId must be a positive decimal identifier");
        }
    }
}
