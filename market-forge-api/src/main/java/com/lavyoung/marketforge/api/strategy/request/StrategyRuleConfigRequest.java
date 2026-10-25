package com.lavyoung.marketforge.api.strategy.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.io.Serializable;

/**
 * 策略规则配置请求项。
 *
 * <p>规则模型决定且只允许携带一种结构化配置：黑名单使用 {@code blacklist}，
 * 权重奖池使用 {@code weight}，次数锁使用 {@code lock}。规则作用域不再由客户端提交
 * {@code ruleType}：黑名单和权重固定属于策略，次数锁固定属于奖品。</p>
 *
 * @param awardId  奖品业务标识；仅次数锁规则需要填写
 * @param ruleModel 规则模型编码
 * @param blacklist 黑名单结构化配置；仅黑名单规则填写
 * @param weight   权重奖池结构化配置；仅权重规则填写
 * @param lock     次数锁结构化配置；仅次数锁规则填写
 * @param ruleDesc 规则运营说明
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Schema(description = "策略规则配置")
public record StrategyRuleConfigRequest(
        @Schema(description = "关联奖品标识；仅次数锁规则填写", example = "900901014", type = "string")
        @Positive(message = "奖品标识必须为正数")
        Long awardId,

        @Schema(description = "规则模型编码", example = "rule_lock", allowableValues = {"rule_blacklist", "rule_weight", "rule_lock"}, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "规则模型不能为空") @Size(max = 32, message = "规则模型长度不能超过 32 个字符")
        @Pattern(regexp = "^(rule_blacklist|rule_weight|rule_lock)$", message = "规则模型不支持结构化配置")
        String ruleModel,

        @Schema(description = "黑名单规则配置") @Valid
        BlacklistRuleConfigRequest blacklist,

        @Schema(description = "权重奖池规则配置") @Valid
        WeightRuleConfigRequest weight,

        @Schema(description = "次数锁规则配置") @Valid
        LockRuleConfigRequest lock,

        @Schema(description = "规则说明", example = "累计抽奖一次后解锁", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "规则说明不能为空") @Size(max = 256, message = "规则说明长度不能超过 256 个字符")
        String ruleDesc
) implements Serializable {

    /**
     * 校验规则模型、作用域和结构化配置是否一一对应。
     *
     * <p>该约束只负责 HTTP 请求结构，奖品引用、重复档位等需要完整聚合上下文的
     * 业务约束仍由领域发布校验器处理。</p>
     *
     * @return 规则模型只携带对应配置，且策略级规则不绑定奖品、奖品级规则已绑定奖品时返回 {@code true}
     */
    @AssertTrue(message = "规则模型、关联奖品与结构化配置不匹配")
    @Schema(hidden = true)
    public boolean isConfigurationValid() {
        if (ruleModel == null) {
            return false;
        }
        int configuredValues = (blacklist == null ? 0 : 1) + (weight == null ? 0 : 1) + (lock == null ? 0 : 1);
        if (configuredValues != 1) {
            return false;
        }
        return switch (ruleModel) {
            case "rule_blacklist" -> awardId == null && blacklist != null;
            case "rule_weight" -> awardId == null && weight != null;
            case "rule_lock" -> awardId != null && lock != null;
            default -> false;
        };
    }
}
