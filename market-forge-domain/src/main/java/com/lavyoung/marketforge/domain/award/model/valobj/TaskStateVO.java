package com.lavyoung.marketforge.domain.award.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/23
 */
@Getter
@AllArgsConstructor
public enum TaskStateVO {

    CREATE("create", "创建"),
    COMPLETED("completed", "已完成"),
    FAIL("fail", "失败"),

    ;
    private final String code;

    private final String desc;
}
