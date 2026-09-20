package com.lavyoung.marketforge.domain.activity.model.aggregate;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityOrderEntity;
import lombok.Builder;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
@Builder
public record CreateOrderAggregate(
        String userId,
        Long activityId,
        Integer totalCount,
        Integer dayCount,
        Integer monthCount,
        ActivityOrderEntity activityOrder
) {
}
