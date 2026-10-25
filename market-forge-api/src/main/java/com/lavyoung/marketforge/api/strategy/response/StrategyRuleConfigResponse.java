package com.lavyoung.marketforge.api.strategy.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 策略规则配置响应。
 *
 * @param awardId    奖品业务标识；策略级规则为空
 * @param ruleType   规则类型：1-策略规则，2-奖品规则
 * @param ruleModel  规则模型编码
 * @param ruleValue  规则配置值
 * @param ruleDesc   规则说明
 * @param createTime 创建时间
 * @param updateTime 最后修改时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Schema(description = "策略规则配置")
public record StrategyRuleConfigResponse(
        @Schema(description = "奖品业务标识；策略级规则为空", example = "900901014", type = "string") String awardId,
        @Schema(description = "规则类型", example = "2") Integer ruleType,
        @Schema(description = "规则模型", example = "rule_lock") String ruleModel,
        @Schema(description = "规则配置值", example = "1") String ruleValue,
        @Schema(description = "规则说明", example = "累计抽奖一次后解锁") String ruleDesc,
        @Schema(description = "创建时间", type = "string", format = "date-time") LocalDateTime createTime,
        @Schema(description = "最后修改时间", type = "string", format = "date-time") LocalDateTime updateTime
) implements Serializable {
}
