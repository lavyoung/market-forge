package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivityAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivityCountAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivitySkuAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivityCountDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivityDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivitySkuDao;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivitySkuPO;
import com.lavyoung.marketforge.infrastructure.persistent.redis.IRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

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

    private IActivityDao activityDao;
    private IActivitySkuDao activitySkuDao;
    private IActivityCountDao activityCountDao;

    private ActivityAssembler activityAssembler;
    private ActivitySkuAssembler activitySkuAssembler;
    private ActivityCountAssembler activityCountAssembler;

    private IRedisService redisService;

    @Override
    public ActivitySkuEntity queryActivitySku(Long sku) {
        Optional<ActivitySkuPO> activitySkuPO = activitySkuDao.queryBySku(sku);
        return activitySkuAssembler.toEntity(activitySkuPO.orElse(null));
    }

    @Override
    public ActivityEntity getActivityEntityByIdActivityId(Long activityId) {
        return activityAssembler.toEntity(activityDao.selectOne(Wrappers.lambdaQuery(ActivityPO.class).eq(ActivityPO::getActivityId, activityId)));
    }

    @Override
    public ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId) {
        return activityCountAssembler.toEntity(activityCountDao.queryByActivityCountId(activityCountId).orElse(null));
    }
}
