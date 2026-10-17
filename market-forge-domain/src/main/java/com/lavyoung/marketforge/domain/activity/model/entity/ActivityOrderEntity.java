package com.lavyoung.marketforge.domain.activity.model.entity;

import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 抽奖活动订单领域实体。
 *
 * @param userId           用户标识
 * @param activityId       抽奖活动标识
 * @param awardUnlockScope 奖品次数锁统计范围，用于向策略规则树透传活动配置
 * @param sku              商品 SKU
 * @param activityName     下单时的活动名称快照
 * @param strategyId       活动关联的抽奖策略标识
 * @param endDateTime      活动结束时间，用于策略库存锁计算过期时间
 * @param orderId          业务订单号
 * @param orderTime        业务下单时间
 * @param totalCount       订单授予的总抽奖次数
 * @param dayCount         订单授予的日抽奖次数
 * @param monthCount       订单授予的月抽奖次数
 * @param state            订单状态
 * @param outBusinessNo    业务幂等ID
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Builder
public record ActivityOrderEntity(
        String userId,
        Long activityId,
        AwardUnlockScopeVO awardUnlockScope,
        Long sku,
        String activityName,
        Long strategyId,
        LocalDateTime endDateTime,
        String orderId,
        LocalDateTime orderTime,
        Integer totalCount,
        Integer dayCount,
        Integer monthCount,
        String state,
        String outBusinessNo
) {

    /**
     * 使用当前活动配置补齐抽奖运行上下文。
     *
     * <p>活动订单持久化表只保存订单事实，不重复保存活动规则配置。
     * 当系统复用数据库中的未消费订单时，通过当前活动实体重新补齐
     * 解锁统计范围和活动结束时间。</p>
     *
     * @param unlockScope         当前活动配置的奖品解锁统计范围
     * @param activityEndDateTime 当前活动结束时间
     * @return 携带完整抽奖运行上下文的新订单实体
     * @throws NullPointerException 解锁范围或活动结束时间为空时抛出
     */
    public ActivityOrderEntity withActivityContext(AwardUnlockScopeVO unlockScope, LocalDateTime activityEndDateTime) {
        return new ActivityOrderEntity(
                userId,
                activityId,
                Objects.requireNonNull(unlockScope, "unlockScope must not be null"),
                sku,
                activityName,
                strategyId,
                Objects.requireNonNull(activityEndDateTime, "activityEndDateTime must not be null"),
                orderId,
                orderTime,
                totalCount,
                dayCount,
                monthCount,
                state,
                outBusinessNo
        );
    }
}
