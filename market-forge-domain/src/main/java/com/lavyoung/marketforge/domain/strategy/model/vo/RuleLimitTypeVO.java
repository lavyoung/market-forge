package com.lavyoung.marketforge.domain.strategy.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 规则限定类型
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
@Getter
@AllArgsConstructor
public enum RuleLimitTypeVO {

    EQ(1, "等于"),
    GT(2, "大于"),
    LT(3, "小于"),
    GE(4, "大于等于"),
    LE(5, "小于等于"),
    ENUM(6, "枚举"),
    NE(7, "不等于"),

    ;
    private final int value;
    private final String info;
}
