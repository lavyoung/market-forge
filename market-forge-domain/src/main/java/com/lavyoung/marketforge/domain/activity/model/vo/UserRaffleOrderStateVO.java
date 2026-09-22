package com.lavyoung.marketforge.domain.activity.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 用户抽奖参与订单状态值对象。
 * <p>
 * 描述抽奖单从创建、使用到作废的生命周期状态。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/22
 */
@Getter
@AllArgsConstructor
public enum UserRaffleOrderStateVO {

    CREATE("create", "创建"),
    USED("used", "已使用"),
    CANCEL("cancel", "已作废"),

    ;

    private final String code;
    private final String desc;
}
