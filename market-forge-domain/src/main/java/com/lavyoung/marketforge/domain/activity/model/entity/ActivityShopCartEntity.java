package com.lavyoung.marketforge.domain.activity.model.entity;

import lombok.Builder;

/**
 * 用户参与抽奖活动的商品选择。
 *
 * @param sku        商品 SKU
 * @param activityId 抽奖活动标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Builder
public record ActivityShopCartEntity(
        Long sku,
        Long activityId
) {
}
