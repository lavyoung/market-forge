package com.lavyoung.marketforge.domain.strategy.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 规则检查结果类型。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/04
 */
@Getter
@AllArgsConstructor
public enum RuleLogicCheckTypeVO {


    /**
     * 放行，继续执行后续抽奖流程。
     */
    ALLOW("0000", "放行；执行后续的流程，不受规则引擎影响"),

    /**
     * 接管，由当前规则的执行结果决定后续流程。
     */
    TAKE_OVER("0001", "接管；后续的流程，受规则引擎执行结果影响"),

    ;

    /**
     * 规则检查结果编码。
     */
    private final String code;

    /**
     * 规则检查结果说明。
     */
    private final String info;
}
