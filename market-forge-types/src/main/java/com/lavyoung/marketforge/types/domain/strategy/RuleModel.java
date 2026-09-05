package com.lavyoung.marketforge.types.domain.strategy;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 抽奖策略支持的规则模型。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
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
    LUCK_AWARD("rule_luck_award"),

    /**
     * 黑名单
     */
    RULE_BLACKLIST("rule_blacklist"),

    ;

    /**
     * 规则模型编码。
     */
    private final String code;

    /**
     * 根据规则模型编码查找枚举值。
     *
     * @param value 规则模型编码
     * @return 匹配的规则模型；不存在时返回 {@code null}
     */
    public static RuleModel get(String value) {
        for (RuleModel ruleModel : RuleModel.values()) {
            if (ruleModel.code.equals(value)) {
                return ruleModel;
            }
        }
        return null;
    }

}
