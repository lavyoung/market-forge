package com.lavyoung.marketforge.api.strategy.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 策略定义运营列表摘要响应。
 *
 * <p>该响应只包含策略主表信息，不包含奖品和规则配置。运营人员打开详情页时，
 * 再通过详情接口读取完整聚合，避免分页查询产生 N+1。</p>
 *
 * @param strategyId   策略业务标识，以字符串返回以避免 JavaScript 整数精度丢失
 * @param strategyDesc 策略描述
 * @param ruleModels   策略级规则模型
 * @param state        生命周期状态
 * @param version      乐观锁版本
 * @param createdBy    创建人
 * @param updatedBy    最后修改人
 * @param createTime   创建时间
 * @param updateTime   最后修改时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@Schema(description = "策略定义列表摘要")
public record StrategyDefinitionSummaryResponse(
        @Schema(description = "策略业务标识", example = "900901001", type = "string") String strategyId,
        @Schema(description = "策略描述", example = "九宫格抽奖策略") String strategyDesc,
        @Schema(description = "策略级规则模型", example = "rule_blacklist,rule_weight") String ruleModels,
        @Schema(description = "生命周期状态", example = "active", allowableValues = {"draft", "active", "disabled"}) String state,
        @Schema(description = "乐观锁版本", example = "2") int version,
        @Schema(description = "创建人", example = "admin") String createdBy,
        @Schema(description = "最后修改人", example = "operator") String updatedBy,
        @Schema(description = "创建时间", type = "string", format = "date-time") LocalDateTime createTime,
        @Schema(description = "最后修改时间", type = "string", format = "date-time") LocalDateTime updateTime
) implements Serializable {
}
