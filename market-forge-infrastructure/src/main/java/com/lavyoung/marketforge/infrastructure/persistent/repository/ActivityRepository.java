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

    /**
     * 按 SKU 查询活动商品配置。
     *
     * @param sku 活动 SKU
     * @return 活动 SKU 领域实体；不存在时返回 {@code null}
     */
    @Override
    public ActivitySkuEntity queryActivitySku(Long sku) {
        Optional<ActivitySkuPO> activitySkuPO = activitySkuDao.queryBySku(sku);
        return activitySkuAssembler.toEntity(activitySkuPO.orElse(null));
    }

    /**
     * 按活动标识查询活动详情，并在缓存缺失时回源数据库。
     *
     * @param activityId 活动标识
     * @return 活动领域实体
     * @throws BusinessException 当活动不存在时抛出
     */
    @Override
    public ActivityEntity getActivityEntityByIdActivityId(Long activityId) {
        return redisService.getValue(Constants.RedisKeys.ACTIVITY_DETAIL_KEY + activityId, ActivityEntity.class).orElseGet(() -> {
            Optional<ActivityPO> po = activityDao.queryByActivityId(activityId);
            if (po.isPresent()) {
                ActivityEntity entity = activityAssembler.toEntity(po.get());
                redisService.setValue(Constants.RedisKeys.ACTIVITY_DETAIL_KEY + activityId, entity);
                return entity;
            } else {
                throw BusinessException.of(BusinessResponseCode.LOTTERY_ACTIVITY_NOT_FOUND, activityId);
            }
        });
    }

    /**
     * 按活动次数配置标识查询次数配置。
     *
     * @param activityCountId 活动次数配置标识
     * @return 活动次数配置领域实体；不存在时返回 {@code null}
     */
    @Override
    public ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId) {
        return activityCountAssembler.toEntity(activityCountDao.queryByActivityCountId(activityCountId).orElse(null));
    }

    /**
     * 保存 SKU 充值订单并累计用户活动账户额度。
     * <p>
     * 该方法在同一事务内写入活动订单，并根据用户活动账户是否已存在执行创建或累加。账户写入失败
     * 时抛出业务异常，确保调用方感知入账失败。
     *
     * @param createQuotaOrderAggregate 创建额度订单聚合
     * @return 活动额度订单号
     * @throws BusinessException 当账户创建或更新失败时抛出
     */
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
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_ACCOUNT_PROCESS_FAILED,
                    createQuotaOrderAggregate.userId(),
                    createQuotaOrderAggregate.activityId(),
                    activityOrderEntity.sku(),
                    activityOrderEntity.orderId(),
                    activityOrderEntity.outBusinessNo());
        }
        return assemblerPO.getOrderId();
    }

    /**
     * 缓存活动 SKU 库存数量。
     *
     * @param cacheKey   库存缓存键
     * @param stockCount 库存数量
     */
    @Override
    public void cacheActivitySkuStockCount(String cacheKey, Integer stockCount) {
        redisService.setValue(cacheKey, stockCount);
    }

    /**
     * 在缓存侧预扣活动 SKU 库存。
     * <p>
     * 方法会先读取 Redis 库存计数，在有库存时为当前库存位置加锁，再通过 CAS 扣减计数；库存耗尽
     * 时发送库存清零事件，交给异步流程同步数据库库存。
     *
     * @param sku         活动 SKU
     * @param cacheKey    库存缓存键
     * @param endDateTime 活动结束时间，用于计算库存锁过期时间
     * @return 预扣成功返回 {@code true}；库存不足或 CAS 失败返回 {@code false}
     */
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
        // 扣减缓存库存
        return redisService.compareAndSetAtomicLong(cacheKey, surplus, surplus - 1);
    }

    /**
     * 查询活动并刷新活动详情缓存。
     *
     * @param activityId 活动标识
     * @throws BusinessException 当活动不存在时抛出
     */
    @Override
    public void queryRaffleActivityByActivityId(Long activityId) {
        Optional<ActivityPO> po = activityDao.queryByActivityId(activityId);
        if (po.isPresent()) {
            ActivityEntity entity = activityAssembler.toEntity(po.get());
            redisService.setValue(Constants.RedisKeys.ACTIVITY_DETAIL_KEY + activityId, entity);
        } else {
            throw BusinessException.of(BusinessResponseCode.LOTTERY_ACTIVITY_NOT_FOUND, activityId);
        }
    }

    /**
     * 发布活动 SKU 库存扣减消息。
     * <p>
     * 消息发布失败时写入延迟补偿队列，避免数据库库存同步因短暂消息异常而永久丢失。
     *
     * @param activitySkuStockKeyVO 活动 SKU 库存同步消息键
     */
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
                    Constants.RedisKeys.ACTIVITY_SKU_STOCK_QUEUE,
                    activitySkuStockKeyVO,
                    Duration.ofSeconds(3)
            );
        }
    }

    /**
     * 查询用户当前未使用的抽奖参与订单。
     *
     * @param partakeRaffleActivity 用户参与活动入参
     * @return 未使用的参与订单；不存在时返回 {@code null}
     */
    @Override
    public ActivityOrderEntity queryNotUsedRaffleOrder(PartakeRaffleActivityEntity partakeRaffleActivity) {
        return activityOrderAssembler.toEntity(activityOrderDao.selectOne(Wrappers.lambdaQuery(ActivityOrderPO.class)
                .eq(ActivityOrderPO::getUserId, partakeRaffleActivity.userId())
                .eq(ActivityOrderPO::getActivityId, partakeRaffleActivity.activityId())
                .eq(ActivityOrderPO::getState, UserRaffleOrderStateVO.CREATE.getCode())
        ));
    }

    /**
     * 查询用户活动总账户。
     *
     * @param userId     用户标识
     * @param activityId 活动标识
     * @return 用户活动总账户；不存在时返回 {@code null}
     */
    @Override
    public ActivityAccountEntity queryActivityAccountByUserId(String userId, Long activityId) {
        return activityAccountAssembler.toEntity(activityAccountDao.selectOne(Wrappers.lambdaQuery(ActivityAccountPO.class)
                .eq(ActivityAccountPO::getActivityId, activityId)
                .eq(ActivityAccountPO::getUserId, userId)
        ));
    }

    /**
     * 查询用户活动月账户。
     *
     * @param userId     用户标识
     * @param activityId 活动标识
     * @param yearMonth  账户归属月份
     * @return 用户活动月账户；不存在时返回 {@code null}
     */
    @Override
    public ActivityAccountMonthEntity queryActivityAccountMonthByUserId(String userId, Long activityId, YearMonth yearMonth) {
        Optional<ActivityAccountMonthPO> activityAccountMonthPO = activityAccountMonthDao.queryByUserIdAndActivityIdAndMonth(
                userId,
                activityId,
                yearMonth.toString()
        );
        return activityAccountMonthAssembler.toEntity(activityAccountMonthPO.orElse(null));
    }

    /**
     * 查询用户活动日账户。
     *
     * @param userId     用户标识
     * @param activityId 活动标识
     * @param localDate  账户归属日期
     * @return 用户活动日账户；不存在时返回 {@code null}
     */
    @Override
    public ActivityAccountDayEntity queryActivityAccountDayByUserId(String userId, Long activityId, LocalDate localDate) {
        Optional<ActivityAccountDayPO> activityAccountDayPO = activityAccountDayDao.queryByUserIdAndActivityIdAndDay(
                userId,
                activityId,
                localDate
        );
        return activityAccountDayAssembler.toEntity(activityAccountDayPO.orElse(null));
    }

    /**
     * 保存抽奖参与订单并扣减用户活动账户额度。
     * <p>
     * 该方法在同一事务中扣减总账户、月账户、日账户额度，并写入一笔待抽奖参与订单。任一账户扣减
     * 或订单落库失败都会抛出业务异常并回滚事务。
     *
     * @param createPartakeOrderAggregate 创建参与订单聚合
     * @param activityOrderEntity         待保存的活动参与订单
     * @throws BusinessException 当额度不足或订单创建失败时抛出
     */
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
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_ACCOUNT_QUOTA_NOT_ENOUGH,
                    userId, activityId, activityOrderEntity.orderId(), activityOrderEntity.sku());
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
                throw BusinessException.of(BusinessResponseCode.ACTIVITY_ACCOUNT_QUOTA_MONTH_NOT_ENOUGH,
                        userId, activityId, activityAccountMonthEntity.month(), activityOrderEntity.orderId(), activityOrderEntity.sku());
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
                throw BusinessException.of(BusinessResponseCode.ACTIVITY_ACCOUNT_QUOTA_DAY_NOT_ENOUGH,
                        userId, activityId, activityAccountDayEntity.day(), activityOrderEntity.orderId(), activityOrderEntity.sku());
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
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_ORDER_CREATE_FAILED,
                    userId, activityId, order.sku(), order.orderId());
        }
    }

    /**
     * 从活动 SKU 库存延迟队列中取出一条待处理消息。
     *
     * @return 活动 SKU 库存消息；队列为空时返回 {@code null}
     * @throws Exception 当队列读取失败时抛出
     */
    @Override
    public ActivitySkuStockKeyVO takeQueueValue() throws Exception {
        return redisService.pollDelayed(
                Constants.RedisKeys.ACTIVITY_SKU_STOCK_QUEUE,
                ActivitySkuStockKeyVO.class
        ).orElse(null);
    }

    /**
     * 扣减数据库侧活动 SKU 库存。
     *
     * @param sku 活动 SKU
     * @throws BusinessException 当 SKU 不存在或数据库库存扣减失败时抛出
     */
    @Override
    public void updateActivitySkuStock(Long sku) {
        ActivitySkuPO activitySkuPO = activitySkuDao.queryBySku(sku)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_NOT_FOUND, sku));
        int updateCount = activitySkuDao.decrementStockCount(
                activitySkuPO.getSku(),
                activitySkuPO.getActivityId(),
                activitySkuPO.getActivityCountId()
        );
        if (updateCount <= 0) {
            log.warn("活动 SKU 库存扣减失败 sku={} activityId={} activityCountId={}",
                    activitySkuPO.getSku(), activitySkuPO.getActivityId(), activitySkuPO.getActivityCountId());
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_STOCK_NOT_ENOUGH,
                    activitySkuPO.getSku(), activitySkuPO.getActivityId(), activitySkuPO.getActivityCountId());
        }
    }

    /**
     * 清空指定活动 SKU 的数据库库存和缓存库存。
     *
     * @param sku 活动 SKU
     * @throws BusinessException 当 SKU 不存在时抛出
     */
    @Override
    public void clearActivitySkuStock(Long sku) {
        ActivitySkuPO activitySkuPO = activitySkuDao.queryBySku(sku)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.ACTIVITY_SKU_NOT_FOUND, sku));
        activitySkuPO.setStockCount(0L);
        activitySkuDao.updateById(activitySkuPO);
        redisService.delete(Constants.RedisKeys.ACTIVITY_SKU_STOCK_COUNT_KEY + sku);
    }
}
