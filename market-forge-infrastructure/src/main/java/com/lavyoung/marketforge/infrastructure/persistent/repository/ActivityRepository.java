package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;
import com.lavyoung.marketforge.domain.strategy.repository.IActivityRepository;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivityOrderDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Repository
@RequiredArgsConstructor
public class ActivityRepository implements IActivityRepository {

    private IActivityOrderDao activityOrderDao;

    @Override
    public ActivitySkuEntity queryActivitySku(String sku) {
        // 
        return null;
    }

    @Override
    public ActivityEntity getActivityEntityByIdActivityId(Long aLong) {
        return null;
    }

    @Override
    public ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long aLong) {
        return null;
    }
}
