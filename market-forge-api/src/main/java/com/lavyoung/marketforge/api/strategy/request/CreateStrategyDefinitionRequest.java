package com.lavyoung.marketforge.api.strategy.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.List;

/**
 * 创建策略定义请求。
 *
 * <p>策略标识、状态、版本和审计操作人由服务端生成。奖品与规则允许为空集合，
 * 支持运营人员先创建草稿再分步完善，但空奖池不能发布。</p>
 *
 * @param strategyDesc 策略描述
 * @param ruleModels   使用逗号分隔的策略级规则模型
 * @param awards       完整策略奖品集合
 * @param rules        完整策略规则集合
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Schema(description = "创建策略定义请求")
public record CreateStrategyDefinitionRequest(
        @Schema(description = "策略描述", example = "九宫格抽奖策略", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "策略描述不能为空") @Size(max = 256, message = "策略描述长度不能超过 256 个字符")
        String strategyDesc,

        @Schema(description = "策略级规则模型", example = "rule_blacklist,rule_weight")
        @Size(max = 64, message = "策略规则模型长度不能超过 64 个字符")
        @Pattern(regexp = "^(?:rule_[a-z_]+(?:,rule_[a-z_]+)*)?$", message = "策略规则模型格式不合法")
        String ruleModels,

        @Schema(description = "策略奖品列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "策略奖品列表不能为空") @Size(max = 100, message = "单个策略最多配置 100 个奖品") @Valid
        List<StrategyAwardConfigRequest> awards,

        @Schema(description = "策略规则列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "策略规则列表不能为空") @Size(max = 200, message = "单个策略最多配置 200 条规则") @Valid
        List<StrategyRuleConfigRequest> rules
) implements Serializable {
}
