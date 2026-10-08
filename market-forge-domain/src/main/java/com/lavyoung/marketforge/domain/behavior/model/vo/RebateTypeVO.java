package com.lavyoung.marketforge.domain.behavior.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 行为返利类型。
 *
 * <p>返利类型决定后续 MQ 消费方该怎么处理这笔返利：
 * SKU 通常会兑换成活动抽奖次数，积分会进入积分账户。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/08
 */
@AllArgsConstructor
@Getter
public enum RebateTypeVO {

    /**
     * 活动 SKU 返利。
     */
    SKU("sku", "活动SKU"),

    /**
     * 积分返利。
     */
    INTEGRAL("integral", "积分"),

    ;
    private final String code;
    private final String desc;
}
