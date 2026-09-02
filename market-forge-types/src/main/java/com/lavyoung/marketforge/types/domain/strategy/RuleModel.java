package com.lavyoung.marketforge.types.domain.strategy;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 策略模式
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 * @date 2026/09/02
 */
@Getter
@AllArgsConstructor
public enum RuleModel {

    /**
     * 抽奖次数解锁
     */
    LOCK("rule_lock"),

    /**
     * 权重
     */
    WEIGHT("rule_weight"),

    /**
     * 随机值
     */
    RANDOM("rule_random"),

    /**
     * 兜底奖品 幸运奖
     */
    LUCK_AWARD("rule_luck_award");

    private final String code;


    public static RuleModel get(String value) {
        for (RuleModel ruleModel : RuleModel.values()) {
            if (ruleModel.code.equals(value)) {
                return ruleModel;
            }
        }
        return null;
    }

}
