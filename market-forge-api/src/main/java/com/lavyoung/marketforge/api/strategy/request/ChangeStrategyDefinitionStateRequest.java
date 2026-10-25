package com.lavyoung.marketforge.api.strategy.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.io.Serializable;

/**
 * 变更策略定义生命周期请求。
 *
 * @param targetState     目标状态：active 或 disabled
 * @param expectedVersion 调用方最近一次读取的乐观锁版本
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Schema(description = "变更策略定义生命周期请求")
public record ChangeStrategyDefinitionStateRequest(
        @Schema(description = "目标状态", example = "active", allowableValues = {"active", "disabled"}, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "目标状态不能为空")
        @Pattern(regexp = "^(active|disabled)$", message = "目标状态只能是 active 或 disabled")
        String targetState,

        @Schema(description = "期望的乐观锁版本", example = "1", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "期望版本不能小于 1")
        int expectedVersion
) implements Serializable {
}
