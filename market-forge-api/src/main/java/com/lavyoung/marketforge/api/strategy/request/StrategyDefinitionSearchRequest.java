package com.lavyoung.marketforge.api.strategy.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.io.Serializable;

/**
 * 策略定义运营分页查询请求。
 *
 * <p>支持按策略标识精确查询、策略描述前缀查询和生命周期状态查询。
 * 所有过滤条件均可省略，但分页参数必须处于项目统一边界内。</p>
 *
 * @param strategyId   策略业务标识；为空时不限制
 * @param strategyDesc 策略描述前缀；为空时不限制
 * @param state        策略生命周期状态；为空时不限制
 * @param pageNumber   页码，从 1 开始
 * @param pageSize     每页记录数，范围为 1 至 100
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@Schema(description = "策略定义运营分页查询条件")
public record StrategyDefinitionSearchRequest(
        @Schema(description = "策略业务标识，精确匹配", example = "900901001", type = "integer", format = "int64")
        @Positive(message = "策略标识必须为正数")
        Long strategyId,

        @Schema(description = "策略描述前缀", example = "九宫格", maxLength = 256)
        @Size(max = 256, message = "策略描述长度不能超过 256 个字符")
        String strategyDesc,

        @Schema(description = "策略生命周期状态", example = "active", allowableValues = {"draft", "active", "disabled"})
        @Pattern(regexp = "^(?:draft|active|disabled)?$", message = "策略生命周期状态不合法")
        String state,

        @Schema(description = "页码，从 1 开始", example = "1", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "页码不能小于 1")
        int pageNumber,

        @Schema(description = "每页记录数", example = "20", minimum = "1", maximum = "100", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "每页记录数不能小于 1")
        @Max(value = 100, message = "每页记录数不能超过 100")
        int pageSize
) implements Serializable {
}
