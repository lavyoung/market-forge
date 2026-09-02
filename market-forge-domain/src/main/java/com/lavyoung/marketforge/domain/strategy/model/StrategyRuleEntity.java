package com.lavyoung.marketforge.domain.strategy.model;

import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
     * com.lavyoung.marketforge.types.domain.strategy.RuleModel
     */
    private String ruleModel;
    /**
     * 抽奖规则比值
     * <p>
     * rule_lock: 配置抽奖次数解锁某个奖品
     * rule_weight: 达到或者消耗积分到达多少 可选抽奖范围 格式： 积分值1:奖品编号1/奖品编号2/奖品编号3;积分值2:奖品编号2/奖品编号3
     * rule_random: 随机奖
     * rule_luck_award: 兜底的幸运星
     *
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


    public Map<String, List<Long>> getRuleWeightValues() {
        if (RuleModel.WEIGHT.getCode().equals(ruleModel)) {
            return Map.of();
        }

        return null;
    }
}
