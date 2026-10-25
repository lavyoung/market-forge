package com.lavyoung.marketforge.api.strategy.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.io.Serializable;

/**
 * 奖品次数锁规则结构化配置请求。
 *
 * <p>用户完成的有效抽奖次数达到 {@code requiredCount} 后，对应奖品才允许进入
 * 后续库存判断。配置为零表示没有次数门槛。</p>
 *
 * @param requiredCount 解锁奖品所需的最低有效抽奖次数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Schema(description = "奖品次数锁规则结构化配置")
public record LockRuleConfigRequest(
        @Schema(description = "奖品解锁所需次数", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "奖品解锁次数不能为空") @PositiveOrZero(message = "奖品解锁次数不能小于零")
        Integer requiredCount
) implements Serializable {
}
