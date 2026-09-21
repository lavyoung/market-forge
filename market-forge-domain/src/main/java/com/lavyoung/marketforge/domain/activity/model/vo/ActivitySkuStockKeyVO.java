package com.lavyoung.marketforge.domain.activity.model.vo;

import lombok.Builder;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
@Builder
public record ActivitySkuStockKeyVO(
        Long sku,
        Long activityId,
        String userId
) {
}
