package com.lavyoung.marketforge.domain.activity.model.entity;

import lombok.Builder;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Builder
public record ActivitySkuEntity(
        Long activityId,
        Long activityCountId
) {
}
