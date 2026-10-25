package com.lavyoung.marketforge.api.strategy.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.io.Serializable;

/**
 * 策略规则配置请求项。
 *
 * <p>{@code ruleType} 为 1 时表示策略级规则，此时 {@code awardId} 应为空；
 * 为 2 时表示奖品级规则，此时 {@code awardId} 必须引用当前策略中的奖品。
 * 该跨字段约束由发布就绪校验器统一判断。</p>
 *
 * @param awardId   奖品业务标识；策略级规则为空
 * @param ruleType  规则类型：1-策略规则，2-奖品规则
 * @param ruleModel 规则模型编码
 * @param ruleValue 规则配置值
 * @param ruleDesc  规则运营说明
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Schema(description = "策略规则配置")
public record StrategyRuleConfigRequest(
        @Schema(description = "奖品业务标识；策略级规则为空", example = "900901014", type = "string")
        @Positive(message = "奖品标识必须为正数")
        Long awardId,

        @Schema(description = "规则类型", example = "2", allowableValues = {"1", "2"}, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "规则类型不能为空") @Min(value = 1, message = "规则类型只能是 1 或 2")
        @Max(value = 2, message = "规则类型只能是 1 或 2")
        Integer ruleType,

        @Schema(description = "规则模型编码", example = "rule_lock", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "规则模型不能为空") @Size(max = 32, message = "规则模型长度不能超过 32 个字符")
        @Pattern(regexp = "^rule_[a-z_]+$", message = "规则模型格式不合法")
        String ruleModel,

        @Schema(description = "规则配置值", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "规则配置值不能为空") @Size(max = 256, message = "规则配置值长度不能超过 256 个字符")
        String ruleValue,

        @Schema(description = "规则说明", example = "累计抽奖一次后解锁", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "规则说明不能为空") @Size(max = 256, message = "规则说明长度不能超过 256 个字符")
        String ruleDesc
) implements Serializable {
}
