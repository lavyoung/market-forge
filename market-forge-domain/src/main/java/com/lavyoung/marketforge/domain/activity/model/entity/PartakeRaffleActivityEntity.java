package com.lavyoung.marketforge.domain.activity.model.entity;

/**
 * 用户参与抽奖活动的入参实体。
 *
 * @param userId     用户标识
 * @param activityId 活动标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/22
 */
public record PartakeRaffleActivityEntity(
        String userId,
        Long activityId
) {
}
