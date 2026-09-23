package com.lavyoung.marketforge.domain.activity.model.entity;

/**
 * 用户参与抽奖活动的入参实体。
 * <p>
 * 用于承载用户发起一次抽奖参与请求所需的最小上下文。{@code sku} 表示本次参与所消耗的
 * 活动权益来源，服务层会校验该 SKU 是否归属于当前活动，避免活动和权益来源错配。
 *
 * @param userId     用户标识
 * @param sku        活动 SKU
 * @param activityId 活动标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/22
 */
public record PartakeRaffleActivityEntity(
        String userId,
        Long sku,
        Long activityId
) {
}
