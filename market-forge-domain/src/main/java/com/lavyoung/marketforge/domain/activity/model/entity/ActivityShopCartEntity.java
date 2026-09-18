package com.lavyoung.marketforge.domain.activity.model.entity;

import lombok.Builder;

/**
 *
 * 活动sku对象
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Builder
public record ActivityShopCartEntity(
        String sku,
        Long activityId
) {
}
