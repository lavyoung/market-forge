package com.lavyoung.marketforge.domain.activity.service.armory.impl;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.armory.IActivityArmory;
import com.lavyoung.marketforge.domain.activity.service.armory.IActivityDispatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityArmoryImpl implements IActivityArmory, IActivityDispatch {

    private final IActivityRepository activityRepository;

    @Override
    public boolean assembleActivitySku(Long sku) {
        ActivitySkuEntity activitySkuEntity = activityRepository.queryActivitySku(sku);
        log.info("组装活动SKU sku={}, activityId={}, activityCountId={}, stockCount={}", sku,
                activitySkuEntity.activityId(), activitySkuEntity.activityCountId(), activitySkuEntity.stockCount());
        return false;
    }
}
