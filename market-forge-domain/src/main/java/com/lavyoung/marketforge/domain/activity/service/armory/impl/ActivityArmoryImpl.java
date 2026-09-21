package com.lavyoung.marketforge.domain.activity.service.armory.impl;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.armory.IActivityArmory;
import com.lavyoung.marketforge.domain.activity.service.armory.IActivityDispatch;
import com.lavyoung.marketforge.types.common.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

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
        cacheActivitySkuStockCount(sku, activitySkuEntity.stockCount());
        // 预热获取 保存到缓存中
        activityRepository.queryRaffleActivityByActivityId(activitySkuEntity.activityId());
        // 预热活动次数【查询时预热到缓存】
        activityRepository.queryRaffleActivityCountByActivityCountId(activitySkuEntity.activityCountId());

        return false;
    }

    private void cacheActivitySkuStockCount(Long sku, Integer stockCount) {
        String cacheKey = Constants.RedisKeys.ACTIVITY_SKU_STOCK_COUNT_KEY + sku;
        activityRepository.cacheActivitySkuStockCount(cacheKey, stockCount);
    }

    @Override
    public boolean subtractionActivitySkuStock(Long sku, LocalDateTime endDateTime) {
        String cacheKey = Constants.RedisKeys.ACTIVITY_SKU_STOCK_COUNT_KEY + sku;
        return activityRepository.subtractionActivitySkuStock(sku, cacheKey, endDateTime);
    }
}
