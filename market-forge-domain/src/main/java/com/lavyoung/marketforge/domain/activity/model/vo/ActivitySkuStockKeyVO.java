package com.lavyoung.marketforge.domain.activity.model.vo;

import lombok.Builder;

/**
 * 活动 SKU 库存同步消息键。
 * <p>
 * 只携带扣减数据库库存所需的 SKU 和活动标识；库存同步不依赖用户标识，避免消息载荷引入无用字段。
 *
 * @param sku        活动 SKU
 * @param activityId 活动标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
@Builder
public record ActivitySkuStockKeyVO(
        Long sku,
        Long activityId
) {
}
