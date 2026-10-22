package com.lavyoung.marketforge.application.activity.model;

import com.lavyoung.marketforge.types.model.PageRequest;

import java.util.Objects;

/**
 * 活动定义运营分页查询条件。
 *
 * @param activityId   活动业务标识；为空时不限制
 * @param activityName 活动名称前缀；为空时不限制
 * @param state        活动生命周期状态；为空时不限制
 * @param page         分页参数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public record ActivityDefinitionSearchQuery(Long activityId, String activityName, String state, PageRequest page) {

    /**
     * 规范化文本查询条件并校验分页参数。
     *
     * @throws NullPointerException 分页参数为空时抛出
     */
    public ActivityDefinitionSearchQuery {
        activityName = normalize(activityName);
        state = normalize(state);
        Objects.requireNonNull(page, "page must not be null");
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
