package com.lavyoung.marketforge.application.strategy.model;

/**
 * 策略规则配置命令项。
 *
 * <p>策略级规则的 {@code awardId} 为空；奖品级规则必须填写 {@code awardId}，
 * 且对应奖品必须存在于同一条策略定义的奖品集合中。</p>
 *
 * @param awardId   奖品业务标识；策略级规则为空，奖品级规则不能为空
 * @param ruleType  规则类型：1 表示策略级规则，2 表示奖品级规则
 * @param ruleModel 规则模型编码，例如 rule_weight、rule_lock 或 rule_stock
 * @param ruleValue 规则配置值，其格式由具体规则模型定义
 * @param ruleDesc  规则的运营说明；可以为空
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public record StrategyRuleConfigCommand(
        Long awardId,
        Integer ruleType,
        String ruleModel,
        String ruleValue,
        String ruleDesc
) {
}
