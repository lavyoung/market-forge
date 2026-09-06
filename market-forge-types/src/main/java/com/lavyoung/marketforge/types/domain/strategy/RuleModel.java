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
    LOCK("rule_lock", "executing"),

    /**
     * 权重
     */
    WEIGHT("rule_weight", "before"),

    /**
     * 随机值
     */
    RANDOM("rule_random", "after"),

    /**
     * 兜底奖品 幸运奖
     */
    LUCK_AWARD("rule_luck_award", "executing"),

    /**
     * 黑名单
     */
    RULE_BLACKLIST("rule_blacklist", "before"),

    ;

    /**
     * 规则模型编码。
     */
    private final String code;

    /**
     * 规则执行阶段，取值为 {@code before}、{@code executing} 或 {@code after}。
     */
    private final String type;

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

    /**
     * 判断指定规则是否在抽奖前阶段执行。
     *
     * @param code 规则模型编码
     * @return 规则存在且执行阶段为 {@code before} 时返回 {@code true}
     */
    public static boolean isBeforeModel(String code) {
        RuleModel model = get(code);
        return model != null && "before".equals(model.type);
    }

    /**
     * 判断指定规则是否在抽奖执行阶段处理。
     *
     * @param code 规则模型编码
     * @return 规则存在且执行阶段为 {@code executing} 时返回 {@code true}
     */
    public static boolean isExecutingModel(String code) {
        RuleModel model = get(code);
        return model != null && "executing".equals(model.type);
    }

    /**
     * 判断指定规则是否在抽奖后阶段执行。
     *
     * @param code 规则模型编码
     * @return 规则存在且执行阶段为 {@code after} 时返回 {@code true}
     */
    public static boolean isAfterModel(String code) {
        RuleModel model = get(code);
        return model != null && "after".equals(model.type);
    }
}
