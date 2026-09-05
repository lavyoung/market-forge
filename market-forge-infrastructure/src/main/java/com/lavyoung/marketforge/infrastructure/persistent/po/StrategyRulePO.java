package com.lavyoung.marketforge.infrastructure.persistent.po;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 抽奖策略规则表持久化对象。
 * <p>
 * 记录策略级或奖品级规则配置，并继承 {@link BasePO} 的审计字段。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
@Getter
@Setter
@NoArgsConstructor
public class StrategyRulePO extends BasePO {

    /**
     * 数据库自增主键。
     */
    private Long id;
    /**
     * 策略业务标识。
     */
    private Long strategyId;
    /**
     * 奖品业务标识；策略级规则可为空。
     */
    private Long awardId;
    /**
     * 规则类型：{@code 1} 表示策略规则，{@code 2} 表示奖品规则。
     */
    private Integer ruleType;
    /**
     * 抽奖规则模型编码，例如 {@code rule_lock} 或 {@code rule_weight}。
     */
    private String ruleModel;
    /**
     * 规则配置值，具体格式由规则模型决定。
     */
    private String ruleValue;
    /**
     * 规则描述。
     */
    private String ruleDesc;

}
