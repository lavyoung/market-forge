package com.lavyoung.marketforge.domain.strategy.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 策略规则实体
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 * @date 2026/08/31
 */
@Data
@Builder
public class StrategyRuleEntity {

    /**
     * 策略id
     */
    private Long strategyId;
    /**
     * 奖品id
     */
    private Long awardId;
    /**
     * 规则类型：【1-策略规则、2-奖品规则】
     */
    private Integer ruleType;
    /**
     * 抽奖规则类型:【rule_lock】
     */
    private String ruleModel;
    /**
     * 抽奖规则比值
     */
    private String ruleValue;
    /**
     * 抽奖规则描述
     */
    private String ruleDesc;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
