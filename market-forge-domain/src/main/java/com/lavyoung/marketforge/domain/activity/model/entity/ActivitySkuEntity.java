package com.lavyoung.marketforge.domain.activity.model.entity;

import lombok.Builder;

/**
 * 抽奖活动 SKU 领域实体。
 *
 * @param sku             商品 SKU
 * @param activityId      抽奖活动标识
 * @param activityCountId 活动参与次数配置标识
 * @param stockCount      商品库存
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Builder
public record ActivitySkuEntity(
        Long sku,
        Long activityId,
        Long activityCountId,
        Long stockCount
) {
}
