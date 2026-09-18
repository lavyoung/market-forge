package com.lavyoung.marketforge.domain.activity.model.entity;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 抽奖活动订单领域实体。
 *
 * @param userId       用户标识
 * @param activityId   抽奖活动标识
 * @param activityName 下单时的活动名称快照
 * @param strategyId   活动关联的抽奖策略标识
 * @param orderId      业务订单号
 * @param orderTime    业务下单时间
 * @param state        订单状态
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Builder
public record ActivityOrderEntity(
        String userId,
        Long activityId,
        String activityName,
        Long strategyId,
        String orderId,
        LocalDateTime orderTime,
        String state
) {
}
