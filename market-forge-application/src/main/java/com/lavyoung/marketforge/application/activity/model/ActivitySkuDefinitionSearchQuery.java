package com.lavyoung.marketforge.application.activity.model;

import com.lavyoung.marketforge.types.model.PageRequest;

import java.util.Objects;

/**
 * 活动 SKU 运营分页查询条件。
 *
 * @param sku             SKU 业务标识；为空时不限制
 * @param activityId      所属活动标识；为空时不限制
 * @param activityCountId 次数配置标识；为空时不限制
 * @param page            分页窗口
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
public record ActivitySkuDefinitionSearchQuery(Long sku, Long activityId, Long activityCountId, PageRequest page) {

    /**
     * 校验查询标识和分页窗口。
     *
     * @throws NullPointerException     分页窗口为空时抛出
     * @throws IllegalArgumentException 任一查询标识不是正数时抛出
     */
    public ActivitySkuDefinitionSearchQuery {
        Objects.requireNonNull(page, "page must not be null");
        if (sku != null && sku <= 0) {
            throw new IllegalArgumentException("sku must be positive");
        }
        if (activityId != null && activityId <= 0) {
            throw new IllegalArgumentException("activityId must be positive");
        }
        if (activityCountId != null && activityCountId <= 0) {
            throw new IllegalArgumentException("activityCountId must be positive");
        }
    }
}
