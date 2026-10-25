package com.lavyoung.marketforge.application.strategy.model;

import java.time.LocalDateTime;

/**
 * 策略规则配置应用结果。
 *
 * <p>完整描述策略级或奖品级规则，不向接口适配层暴露领域实体。</p>
 *
 * @param awardId    奖品业务标识；策略级规则为空
 * @param ruleType   规则类型：1 表示策略级规则，2 表示奖品级规则
 * @param ruleModel  规则模型编码
 * @param ruleValue  规则配置值
 * @param ruleDesc   规则运营说明
 * @param createTime 规则创建时间
 * @param updateTime 规则最后修改时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public record StrategyRuleConfigResult(
        Long awardId,
        Integer ruleType,
        String ruleModel,
        String ruleValue,
        String ruleDesc,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
