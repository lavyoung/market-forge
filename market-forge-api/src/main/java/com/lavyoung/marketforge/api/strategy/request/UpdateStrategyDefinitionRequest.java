package com.lavyoung.marketforge.api.strategy.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.io.Serializable;
import java.util.List;

/**
 * 修改草稿策略定义请求。
 *
 * <p>请求提交策略根信息、奖品和规则的完整最新快照。策略标识来自 HTTP 路径，
 * 操作人来自认证会话，期望版本用于防止并发修改相互覆盖。</p>
 *
 * @param strategyDesc    最新策略描述
 * @param ruleModels      最新策略级规则模型
 * @param awards          最新策略奖品完整集合
 * @param rules           最新策略规则完整集合
 * @param expectedVersion 调用方最近一次读取的乐观锁版本
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Schema(description = "修改草稿策略定义请求")
public record UpdateStrategyDefinitionRequest(
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
        List<StrategyRuleConfigRequest> rules,

        @Schema(description = "期望的乐观锁版本", example = "1", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "期望版本不能小于 1")
        int expectedVersion
) implements Serializable {
}
