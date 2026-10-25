package com.lavyoung.marketforge.api.strategy.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 策略定义完整响应。
 *
 * <p>一次返回策略根配置、奖品和规则，避免运营编辑页分别读取三类数据后
 * 在浏览器中拼接出不一致快照。所有业务标识均使用字符串响应。</p>
 *
 * @param strategyId   策略业务标识
 * @param strategyDesc 策略描述
 * @param ruleModels   策略级规则模型
 * @param state        生命周期状态
 * @param version      乐观锁版本
 * @param createdBy    创建人
 * @param updatedBy    最后修改人
 * @param createTime   创建时间
 * @param updateTime   最后修改时间
 * @param awards       策略奖品列表
 * @param rules        策略规则列表
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Schema(description = "策略定义")
public record StrategyDefinitionResponse(
        @Schema(description = "策略业务标识", example = "900901001", type = "string") String strategyId,
        @Schema(description = "策略描述", example = "九宫格抽奖策略") String strategyDesc,
        @Schema(description = "策略级规则模型", example = "rule_blacklist,rule_weight") String ruleModels,
        @Schema(description = "生命周期状态", example = "draft", allowableValues = {"draft", "active", "disabled"}) String state,
        @Schema(description = "乐观锁版本", example = "1") int version,
        @Schema(description = "创建人", example = "admin") String createdBy,
        @Schema(description = "最后修改人", example = "admin") String updatedBy,
        @Schema(description = "创建时间", type = "string", format = "date-time") LocalDateTime createTime,
        @Schema(description = "最后修改时间", type = "string", format = "date-time") LocalDateTime updateTime,
        @Schema(description = "策略奖品列表") List<StrategyAwardConfigResponse> awards,
        @Schema(description = "策略规则列表") List<StrategyRuleConfigResponse> rules
) implements Serializable {

    /**
     * 创建响应并保护子集合。
     *
     * @throws NullPointerException 奖品或规则集合为空时抛出
     */
    public StrategyDefinitionResponse {
        awards = List.copyOf(Objects.requireNonNull(awards, "awards must not be null"));
        rules = List.copyOf(Objects.requireNonNull(rules, "rules must not be null"));
    }
}
