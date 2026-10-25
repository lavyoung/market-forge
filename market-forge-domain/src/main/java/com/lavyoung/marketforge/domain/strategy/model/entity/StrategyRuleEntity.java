package com.lavyoung.marketforge.domain.strategy.model.entity;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 抽奖策略规则实体。
 * <p>
 * 描述策略级或奖品级规则，并提供对权重规则配置值的结构化解析能力。
 *
 * @param strategyId 策略标识
 * @param awardId    奖品标识；策略级规则可为空
 * @param ruleType   规则类型：1 为策略规则，2 为奖品规则
 * @param ruleModel  规则模型编码
 * @param ruleValue  规则配置值
 * @param ruleDesc   规则描述
 * @param createTime 创建时间
 * @param updateTime 更新时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/08/31
 */
@Builder
public record StrategyRuleEntity(
        Long strategyId,
        Long awardId,
        Integer ruleType,
        String ruleModel,
        String ruleValue,
        String ruleDesc,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
