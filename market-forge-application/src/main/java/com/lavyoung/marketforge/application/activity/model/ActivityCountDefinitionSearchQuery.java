package com.lavyoung.marketforge.application.activity.model;

import com.lavyoung.marketforge.types.model.PageRequest;

import java.util.Objects;

/**
 * 活动次数配置分页查询条件。
 *
 * @param activityCountId   活动次数配置业务标识；为空时不限制
 * @param activityCountName 配置名称前缀；为空时不限制
 * @param page              分页参数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public record ActivityCountDefinitionSearchQuery(Long activityCountId, String activityCountName, PageRequest page) {

    /**
     * 规范化名称并校验分页参数。
     *
     * @throws NullPointerException 分页参数为空时抛出
     */
    public ActivityCountDefinitionSearchQuery {
        activityCountName = activityCountName == null || activityCountName.isBlank() ? null : activityCountName.trim();
        Objects.requireNonNull(page, "page must not be null");
    }
}
