package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lavyoung.marketforge.domain.activity.model.aggregate.CreateOrderAggregate;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityOrderEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.*;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.*;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityAccountPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityOrderPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivitySkuPO;
import com.lavyoung.marketforge.infrastructure.persistent.redis.IRedisService;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    private final IActivityDao activityDao;
    private final IActivitySkuDao activitySkuDao;
    private final IActivityCountDao activityCountDao;
    private final IActivityOrderDao activityOrderDao;
    private final IActivityAccountDao activityAccountDao;

    private final ActivityAssembler activityAssembler;
    private final ActivitySkuAssembler activitySkuAssembler;
    private final ActivityCountAssembler activityCountAssembler;
    private final ActivityOrderAssembler activityOrderAssembler;
    private final ActivityAccountAssembler activityAccountAssembler;

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

    @Override
    @Transactional
    public String saveOrderAggregate(CreateOrderAggregate createOrderAggregate) {
        ActivityOrderEntity activityOrderEntity = createOrderAggregate.activityOrder();
        // PO对象
        ActivityOrderPO assemblerPO = activityOrderAssembler.toPO(activityOrderEntity);
        // 写入订单
        activityOrderDao.insert(assemblerPO);

        Optional<ActivityAccountPO> accountPOPresent = activityAccountDao.queryByUserIdAndActivityId(createOrderAggregate.userId(), createOrderAggregate.activityId());
        int res;
        ActivityAccountPO activityAccountPO;
        if (accountPOPresent.isPresent()) {
            // 更新
            activityAccountPO = accountPOPresent.get();
            activityAccountPO.setTotalCount(createOrderAggregate.totalCount() + activityAccountPO.getTotalCount());
            activityAccountPO.setTotalCountSurplus(createOrderAggregate.totalCount() + activityAccountPO.getTotalCountSurplus());
            activityAccountPO.setDayCount(createOrderAggregate.dayCount() + activityAccountPO.getDayCount());
            activityAccountPO.setDayCountSurplus(createOrderAggregate.dayCount() + activityAccountPO.getDayCountSurplus());
            activityAccountPO.setMonthCount(createOrderAggregate.monthCount() + activityAccountPO.getMonthCount());
            activityAccountPO.setMonthCountSurplus(createOrderAggregate.monthCount() + activityAccountPO.getMonthCountSurplus());
            activityAccountPO.setVersion(activityAccountPO.getVersion() + 1);
            res = activityAccountDao.updateById(activityAccountPO);
        } else {
            activityAccountPO = new ActivityAccountPO(
                    null,
                    createOrderAggregate.userId(),
                    createOrderAggregate.activityId(),
                    createOrderAggregate.totalCount(),
                    createOrderAggregate.totalCount(),
                    createOrderAggregate.dayCount(),
                    createOrderAggregate.dayCount(),
                    createOrderAggregate.monthCount(),
                    createOrderAggregate.monthCount(),
                    1
            );
            res = activityAccountDao.insert(activityAccountPO);
        }

        if (res <= 0) {
            throw new BusinessException(BusinessResponseCode.ACTIVITY_ACCOUNT_ERR);
        }
        return assemblerPO.getOrderId();
    }
}
