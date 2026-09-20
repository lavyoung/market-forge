package com.lavyoung.marketforge.domain.activity.service.impl;

import com.lavyoung.marketforge.domain.activity.model.aggregate.CreateOrderAggregate;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.SkuRechargeEntity;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.AbstractRaffleActivity;
import com.lavyoung.marketforge.domain.activity.service.rule.factory.DefaultActivityChainFactory;
import org.springframework.stereotype.Service;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
@Service
public class RaffleActivityServiceImpl extends AbstractRaffleActivity {

    public RaffleActivityServiceImpl(IActivityRepository activityRepository, DefaultActivityChainFactory activityChainFactory) {
        super(activityRepository, activityChainFactory);
    }

    @Override
    protected void doSaveOrder(CreateOrderAggregate createOrderAggregate) {
        activityRepository.saveOrderAggregate(createOrderAggregate);
    }

    @Override
    protected CreateOrderAggregate buildOrderAggregate(SkuRechargeEntity skuRechargeEntity, ActivitySkuEntity activitySkuEntity, ActivityEntity activityEntity, ActivityCountEntity activityCountEntity) {
        return null;
    }
}
