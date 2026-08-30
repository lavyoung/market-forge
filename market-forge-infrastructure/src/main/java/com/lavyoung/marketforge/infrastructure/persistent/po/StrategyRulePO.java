package com.lavyoung.marketforge.infrastructure.persistent.po;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 抽奖策略规则持久化对象。
 *
 * @author lavyoung
 * @version 1.0.0-SNAPSHOT
 * @email lavyoung1325@outlook.com
 */
@Getter
@Setter
@NoArgsConstructor
public class StrategyRulePO {

    /**
     * 自增id
     */
    private Long id;
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
