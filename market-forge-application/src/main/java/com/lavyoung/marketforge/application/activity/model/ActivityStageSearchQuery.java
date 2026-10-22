package com.lavyoung.marketforge.application.activity.model;

import com.lavyoung.marketforge.types.model.PageRequest;

import java.util.Objects;

/**
 * 活动展台运营分页查询条件。
 *
 * <p>渠道、来源和生命周期状态均为可选的精确匹配条件。空白字符串会在应用边界
 * 统一转换为 {@code null}，避免基础设施层重复判断 HTTP 参数的空白语义。</p>
 *
 * @param channel 投放渠道；为空时不限制
 * @param source  流量来源；为空时不限制
 * @param state   生命周期状态；为空时不限制
 * @param page    分页参数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public record ActivityStageSearchQuery(String channel, String source, String state, PageRequest page) {

    /**
     * 规范化可选查询条件并校验分页参数。
     *
     * @throws NullPointerException 分页参数为空时抛出
     */
    public ActivityStageSearchQuery {
        channel = normalize(channel);
        source = normalize(source);
        state = normalize(state);
        Objects.requireNonNull(page, "page must not be null");
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
