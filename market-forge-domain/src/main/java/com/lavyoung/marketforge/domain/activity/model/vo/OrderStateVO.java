package com.lavyoung.marketforge.domain.activity.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 活动订单状态值对象。
 * <p>
 * 保留活动额度订单的状态扩展点，具体状态由业务流程逐步补充。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
@Getter
@AllArgsConstructor
public enum OrderStateVO {

    /**
     * 额度充值订单已完成。
     */
    COMPLETE("complete", "已完成"),

    ;

    /**
     * 状态编码，持久化到活动订单表。
     */
    private final String code;

    /**
     * 状态说明，用于日志和后台展示。
     */
    private final String desc;
}
