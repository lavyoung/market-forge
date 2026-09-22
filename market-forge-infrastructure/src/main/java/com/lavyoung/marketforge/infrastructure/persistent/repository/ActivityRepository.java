package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lavyoung.marketforge.domain.activity.event.ActivitySkuStockDeductedEvent;
import com.lavyoung.marketforge.domain.activity.event.ActivitySkuZeroStockEvent;
import com.lavyoung.marketforge.domain.activity.model.aggregate.CreatePartakeOrderAggregate;
import com.lavyoung.marketforge.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import com.lavyoung.marketforge.domain.activity.model.entity.*;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivitySkuStockKeyVO;
import com.lavyoung.marketforge.domain.activity.model.vo.UserRaffleOrderStateVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.*;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.*;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.*;
import com.lavyoung.marketforge.infrastructure.persistent.redis.IRedisService;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.messaging.MessagePublisher;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 活动域仓储实现。
 * <p>
 * 负责活动、SKU、账户和订单的数据库读写，并协调 Redis 库存缓存与 MQ 库存同步消息。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ActivityRepository implements IActivityRepository {

    private final IActivityDao activityDao;
    private final IActivitySkuDao activitySkuDao;
    private final IActivityCountDao activityCountDao;
    private final IActivityOrderDao activityOrderDao;
    private final IActivityAccountDao activityAccountDao;
    private final IActivityAccountDayDao activityAccountDayDao;
    private final IActivityAccountMonthDao activityAccountMonthDao;

    private final ActivityAssembler activityAssembler;
    private final ActivitySkuAssembler activitySkuAssembler;
    private final ActivityCountAssembler activityCountAssembler;
    private final ActivityOrderAssembler activityOrderAssembler;
    private final ActivityAccountAssembler activityAccountAssembler;
    private final ActivityAccountDayAssembler activityAccountDayAssembler;
    private final ActivityAccountMonthAssembler activityAccountMonthAssembler;

    private final IRedisService redisService;
    private final MessagePublisher messagePublisher;

    @Override
    public ActivitySkuEntity queryActivitySku(Long sku) {
        Optional<ActivitySkuPO> activitySkuPO = activitySkuDao.queryBySku(sku);
        return activitySkuAssembler.toEntity(activitySkuPO.orElse(null));
    }

    @Override
    public ActivityEntity getActivityEntityByIdActivityId(Long activityId) {
        return redisService.getValue(Constants.RedisKeys.ACTIVITY_DETAIL_KEY + activityId, ActivityEntity.class).orElseGet(() -> {
            Optional<ActivityPO> po = activityDao.queryByActivityId(activityId);
            if (po.isPresent()) {
                ActivityEntity entity = activityAssembler.toEntity(po.get());
                redisService.setValue(Constants.RedisKeys.ACTIVITY_DETAIL_KEY + activityId, entity);
                return entity;
            } else {
                throw new BusinessException(BusinessResponseCode.LOTTERY_ACTIVITY_NOT_FOUND);
            }
        });
    }

    @Override
    public ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId) {
        return activityCountAssembler.toEntity(activityCountDao.queryByActivityCountId(activityCountId).orElse(null));
    }

    @Override
    @Transactional
    public String saveOrderAggregate(CreateQuotaOrderAggregate createQuotaOrderAggregate) {
        ActivityOrderEntity activityOrderEntity = createQuotaOrderAggregate.activityOrder();
        // PO对象
        ActivityOrderPO assemblerPO = activityOrderAssembler.toPO(activityOrderEntity);
        // 写入订单
        activityOrderDao.insert(assemblerPO);

        Optional<ActivityAccountPO> accountPOPresent = activityAccountDao.queryByUserIdAndActivityId(createQuotaOrderAggregate.userId(), createQuotaOrderAggregate.activityId());
        int res;
        ActivityAccountPO activityAccountPO;
        if (accountPOPresent.isPresent()) {
            // 更新
            activityAccountPO = accountPOPresent.get();
            activityAccountPO.setTotalCount(createQuotaOrderAggregate.totalCount() + activityAccountPO.getTotalCount());
            activityAccountPO.setTotalCountSurplus(createQuotaOrderAggregate.totalCount() + activityAccountPO.getTotalCountSurplus());
            activityAccountPO.setDayCount(createQuotaOrderAggregate.dayCount() + activityAccountPO.getDayCount());
            activityAccountPO.setDayCountSurplus(createQuotaOrderAggregate.dayCount() + activityAccountPO.getDayCountSurplus());
            activityAccountPO.setMonthCount(createQuotaOrderAggregate.monthCount() + activityAccountPO.getMonthCount());
            activityAccountPO.setMonthCountSurplus(createQuotaOrderAggregate.monthCount() + activityAccountPO.getMonthCountSurplus());
            activityAccountPO.setVersion(activityAccountPO.getVersion() + 1);
            res = activityAccountDao.updateById(activityAccountPO);
        } else {
            activityAccountPO = new ActivityAccountPO(
                    null,
                    createQuotaOrderAggregate.userId(),
                    createQuotaOrderAggregate.activityId(),
                    createQuotaOrderAggregate.totalCount(),
                    createQuotaOrderAggregate.totalCount(),
                    createQuotaOrderAggregate.dayCount(),
                    createQuotaOrderAggregate.dayCount(),
                    createQuotaOrderAggregate.monthCount(),
                    createQuotaOrderAggregate.monthCount(),
                    1
            );
            res = activityAccountDao.insert(activityAccountPO);
        }

        if (res <= 0) {
            throw new BusinessException(BusinessResponseCode.ACTIVITY_ACCOUNT_PROCESS_FAILED);
        }
        return assemblerPO.getOrderId();
    }

    @Override
    public void cacheActivitySkuStockCount(String cacheKey, Integer stockCount) {
        redisService.setValue(cacheKey, stockCount);
    }

    @Override
    public boolean subtractionActivitySkuStock(Long sku, String cacheKey, LocalDateTime endDateTime) {
        int surplus = redisService.getValue(cacheKey, Integer.class).orElse(0);
        if (surplus == 0) {
            // 没有库存了 发送mq消息进行库存更新
            messagePublisher.publish(new ActivitySkuZeroStockEvent(
                    sku
            ));
            return false;
        } else if (surplus < 0) {
            redisService.setAtomicLong(cacheKey, 0);
            return false;
        }
        // 这里是为了防止重复扣减一个活动的库存值 一般来说 后续恢复库存没那么快
        // 1. 按照cacheKey decr 后的值，如 99、98、97 和 key 组成为库存锁的key进行使用。
        // 2. 加锁为了兜底，如果后续有恢复库存，手动处理等【运营是人来操作，会有这种情况发放，系统要做防护】，也不会超卖。因为所有的可用库存key，都被加锁了。
        // 3. 设置加锁时间为活动到期 + 延迟1天
        String lockKey = cacheKey + Constants.UNDERLINE + surplus;
        long expireMillis = endDateTime.toInstant(ZoneOffset.of(ZoneId.systemDefault().getId())).toEpochMilli() - System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1);
        boolean setIfAbsent = redisService.setIfAbsent(lockKey, 1, Duration.ofSeconds((expireMillis / 1000) + 1));
        if (!setIfAbsent) {
            log.error("活动sku库存加锁失败 {}", lockKey);
        }
        return true;
    }

    @Override
    public void queryRaffleActivityByActivityId(Long activityId) {
        Optional<ActivityPO> po = activityDao.queryByActivityId(activityId);
        if (po.isPresent()) {
            ActivityEntity entity = activityAssembler.toEntity(po.get());
            redisService.setValue(Constants.RedisKeys.ACTIVITY_DETAIL_KEY + activityId, entity);
        } else {
            throw new BusinessException(BusinessResponseCode.LOTTERY_ACTIVITY_NOT_FOUND);
        }
    }

    @Override
    public void activitySkuStockConsumeSendQueue(ActivitySkuStockKeyVO activitySkuStockKeyVO) {
        try {
            // 发送延迟队列
            messagePublisher.publish(new ActivitySkuStockDeductedEvent(
                    activitySkuStockKeyVO.sku(),
                    activitySkuStockKeyVO.activityId(),
                    activitySkuStockKeyVO.userId()
            ));
        } catch (RuntimeException e) {
            log.error("活动次数扣减事件发布失败，转入补偿队列 sku={} activityId={} userId={}",
                    activitySkuStockKeyVO.sku(), activitySkuStockKeyVO.activityId(), activitySkuStockKeyVO.userId(), e);
            redisService.offerDelayed(
                    Constants.RedisKeys.STRATEGY_AWARD_STOCK_QUEUE,
                    activitySkuStockKeyVO,
                    Duration.ofSeconds(3)
            );
        }
    }

    @Override
    public ActivityOrderEntity queryNotUsedRaffleOrder(PartakeRaffleActivityEntity partakeRaffleActivity) {
        return activityOrderAssembler.toEntity(activityOrderDao.selectOne(Wrappers.lambdaQuery(ActivityOrderPO.class)
                .eq(ActivityOrderPO::getUserId, partakeRaffleActivity.userId())
                .eq(ActivityOrderPO::getActivityId, partakeRaffleActivity.activityId())
                .eq(ActivityOrderPO::getState, UserRaffleOrderStateVO.CREATE.getCode())
        ));
    }

    @Override
    public ActivityAccountEntity queryActivityAccountByUserId(String userId, Long activityId) {
        return activityAccountAssembler.toEntity(activityAccountDao.selectOne(Wrappers.lambdaQuery(ActivityAccountPO.class)
                .eq(ActivityAccountPO::getActivityId, activityId)
                .eq(ActivityAccountPO::getUserId, userId)
        ));
    }

    @Override
    public ActivityAccountMonthEntity queryActivityAccountMonthByUserId(String userId, Long activityId, YearMonth yearMonth) {
        Optional<ActivityAccountMonthPO> activityAccountMonthPO = activityAccountMonthDao.queryByUserIdAndActivityIdAndMonth(
                userId,
                activityId,
                yearMonth.toString()
        );
        return activityAccountMonthAssembler.toEntity(activityAccountMonthPO.orElse(null));
    }

    @Override
    public ActivityAccountDayEntity queryActivityAccountDayByUserId(String userId, Long activityId, LocalDate localDate) {
        Optional<ActivityAccountDayPO> activityAccountDayPO = activityAccountDayDao.queryByUserIdAndActivityIdAndDay(
                userId,
                activityId,
                localDate
        );
        return activityAccountDayAssembler.toEntity(activityAccountDayPO.orElse(null));
    }

    @Override
    public void saveCreatePartakeOrderAggregate(CreatePartakeOrderAggregate createPartakeOrderAggregate, ActivityOrderEntity activityOrderEntity) {
        String userId = createPartakeOrderAggregate.userId();
        Long activityId = createPartakeOrderAggregate.activityId();
        ActivityAccountDayEntity activityAccountDayEntity = createPartakeOrderAggregate.activityAccountDayEntity();
        ActivityAccountMonthEntity activityAccountMonthEntity = createPartakeOrderAggregate.activityAccountMonthEntity();
        // 更新总账户额度
        int totalAccountRes = activityAccountDao.decrementTotalCountSurplus(
                userId,
                activityId
        );
        if (totalAccountRes != 1) {
            log.warn("写入创建参与活动记录账户 更新总账户额度错误 userId={} activityId={}", userId, activityId);
            throw new BusinessException(BusinessResponseCode.ACTIVITY_ACCOUNT_QUOTA_NOT_ENOUGH);
        }

        // 检查月账单
        if (!createPartakeOrderAggregate.isExistAccountMonth()) {
            // 新增账户 当前是扣一次的 用于生成抽奖单
            ActivityAccountMonthPO activityAccountMonthPO = activityAccountMonthAssembler.toPO(activityAccountMonthEntity);
            activityAccountMonthPO.setMonthCountSurplus(activityAccountMonthPO.getMonthCountSurplus() - 1);
            activityAccountMonthPO.setVersion(1);
            activityAccountMonthDao.insert(activityAccountMonthPO);
        } else {
            int monthAccountRes = activityAccountMonthDao.decrementMonthCountSurplus(
                    userId,
                    activityId,
                    activityAccountMonthEntity.month().toString()
            );
            if (monthAccountRes <= 0) {
                log.warn("写入创建活动参与记录 更新月账户额度不足 userId={}, activityId={} month={}", userId,
                        activityId, activityAccountMonthEntity.month());
                throw new BusinessException(BusinessResponseCode.ACTIVITY_ACCOUNT_QUOTA_MONTH_NOT_ENOUGH);
            }
        }

        // 检查日额度
        if (!createPartakeOrderAggregate.isExistAccountDay()) {
            ActivityAccountDayPO activityAccountDayPO = activityAccountDayAssembler.toPO(activityAccountDayEntity);
            activityAccountDayPO.setDayCountSurplus(activityAccountDayPO.getDayCountSurplus() - 1);
            activityAccountDayPO.setVersion(1);
            activityAccountDayDao.insert(activityAccountDayPO);
        } else {
            int dayAccountRes = activityAccountDayDao.decrementDayCountSurplus(
                    userId,
                    activityId,
                    activityAccountDayEntity.day()
            );
            if (dayAccountRes <= 0) {
                log.warn("写入创建活动参与记录 更新日账户额度不足 userId={}, activityId={} day={}", userId,
                        activityId, activityAccountDayEntity.day());
                throw new BusinessException(BusinessResponseCode.ACTIVITY_ACCOUNT_QUOTA_DAY_NOT_ENOUGH);
            }
        }

        // 创建订单
        ActivityOrderEntity order = new ActivityOrderEntity(
                activityOrderEntity.userId(),
                activityOrderEntity.activityId(),
                activityOrderEntity.sku(),
                activityOrderEntity.activityName(),
                activityOrderEntity.strategyId(),
                activityOrderEntity.orderId(),
                activityOrderEntity.orderTime(),
                1,
                1,
                1,
                activityOrderEntity.state(),
                activityOrderEntity.outBusinessNo()
        );
        ActivityOrderPO activityOrderPO = activityOrderAssembler.toPO(order);
        int insert = activityOrderDao.insert(activityOrderPO);
        if (insert <= 0) {
            log.warn("写入创建活动参与记录 插入活动订单错误 userId={} activity={}, order={}", userId, activityId, order);
            throw new BusinessException(BusinessResponseCode.ACTIVITY_ACCOUNT_QUOTA_DAY_NOT_ENOUGH);
        }
    }
}
