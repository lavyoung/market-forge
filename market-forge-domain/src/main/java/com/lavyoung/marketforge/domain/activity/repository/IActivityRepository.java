package com.lavyoung.marketforge.domain.activity.repository;

import com.lavyoung.marketforge.domain.activity.model.aggregate.CreateOrderAggregate;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
public interface IActivityRepository {

    ActivitySkuEntity queryActivitySku(Long sku);

    ActivityEntity getActivityEntityByIdActivityId(Long activityId);

    ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId);

    String saveOrderAggregate(CreateOrderAggregate createOrderAggregate);
}
