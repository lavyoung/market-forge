package com.lavyoung.marketforge.domain.activity.repository;

import com.lavyoung.marketforge.domain.activity.model.aggregate.CreatePartakeOrderAggregate;
import com.lavyoung.marketforge.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import com.lavyoung.marketforge.domain.activity.model.entity.*;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivitySkuStockKeyVO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * 活动域仓储端口。
 * <p>
 * 屏蔽活动、SKU、账户、订单以及库存缓存的持久化细节，为领域服务提供一致的数据访问能力。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
public interface IActivityRepository {

    /**
     * 按 SKU 查询活动商品配置。
     *
     * @param sku 商品 SKU
     * @return 活动商品实体；不存在时返回 null
     */
    ActivitySkuEntity queryActivitySku(Long sku);

    /**
     * 按活动标识查询活动详情。
     *
     * @param activityId 活动标识
     * @return 活动实体
     */
    ActivityEntity getActivityEntityByIdActivityId(Long activityId);

    /**
     * 按活动次数配置标识查询次数配置。
     *
     * @param activityCountId 活动次数配置标识
     * @return 活动次数配置实体；不存在时返回 null
     */
    ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId);

    /**
     * 保存额度充值订单并累计用户活动账户次数。
     *
     * @param createQuotaOrderAggregate 创建额度订单聚合
     * @return 活动额度订单号
     */
    String saveOrderAggregate(CreateQuotaOrderAggregate createQuotaOrderAggregate);

    /**
     * 缓存活动 SKU 库存。
     *
     * @param cacheKey   库存缓存键
     * @param stockCount 库存数量
     */
    void cacheActivitySkuStockCount(String cacheKey, Integer stockCount);

    /**
     * 从缓存侧预扣活动 SKU 库存。
     *
     * @param sku         商品 SKU
     * @param cacheKey    库存缓存键
     * @param endDateTime 活动结束时间
     * @return 预扣成功返回 true；库存不足返回 false
     */
    boolean subtractionActivitySkuStock(Long sku, String cacheKey, LocalDateTime endDateTime);

    /**
     * 查询活动并写入活动详情缓存。
     *
     * @param activityId 活动标识
     */
    void queryRaffleActivityByActivityId(Long activityId);

    /**
     * 发布活动 SKU 库存消费消息。
     *
     * @param activitySkuStockKeyVO 活动 SKU 库存消息键
     */
    void activitySkuStockConsumeSendQueue(ActivitySkuStockKeyVO activitySkuStockKeyVO);

    /**
     * 查询用户当前未使用的抽奖参与订单。
     *
     * @param partakeRaffleActivity 用户参与活动入参
     * @return 未使用的抽奖订单；不存在时返回 null
     */
    ActivityOrderEntity queryNotUsedRaffleOrder(PartakeRaffleActivityEntity partakeRaffleActivity);

    /**
     * 查询用户活动总账户。
     *
     * @param userId     用户标识
     * @param activityId 活动标识
     * @return 用户活动总账户；不存在时返回 null
     */
    ActivityAccountEntity queryActivityAccountByUserId(String userId, Long activityId);

    /**
     * 查询用户活动月账户。
     *
     * @param userId     用户标识
     * @param activityId 活动标识
     * @param yearMonth  账户归属月份
     * @return 用户活动月账户；不存在时返回 null
     */
    ActivityAccountMonthEntity queryActivityAccountMonthByUserId(String userId, Long activityId, YearMonth yearMonth);

    /**
     * 查询用户活动日账户。
     *
     * @param userId     用户标识
     * @param activityId 活动标识
     * @param localDate  账户归属日期
     * @return 用户活动日账户；不存在时返回 null
     */
    ActivityAccountDayEntity queryActivityAccountDayByUserId(String userId, Long activityId, LocalDate localDate);

    /**
     * 保存抽奖参与订单并扣减用户活动账户额度。
     *
     * @param createPartakeOrderAggregate 创建抽奖参与订单聚合
     * @param activityOrderEntity         抽奖参与订单实体
     */
    void saveCreatePartakeOrderAggregate(CreatePartakeOrderAggregate createPartakeOrderAggregate, ActivityOrderEntity activityOrderEntity);
}
