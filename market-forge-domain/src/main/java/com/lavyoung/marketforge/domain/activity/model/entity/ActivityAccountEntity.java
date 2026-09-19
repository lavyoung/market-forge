package com.lavyoung.marketforge.domain.activity.model.entity;

import lombok.Builder;

/**
 * 用户抽奖活动账户领域实体。
 *
 * @param userId            用户标识
 * @param activityId        抽奖活动标识
 * @param totalCount        累计获得的总次数
 * @param totalCountSurplus 当前剩余总次数
 * @param dayCount          每日可用次数
 * @param dayCountSurplus   当日剩余次数
 * @param monthCount        每月可用次数
 * @param monthCountSurplus 当月剩余次数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Builder
public record ActivityAccountEntity(
        String userId,
        Long activityId,
        Integer totalCount,
        Integer totalCountSurplus,
        Integer dayCount,
        Integer dayCountSurplus,
        Integer monthCount,
        Integer monthCountSurplus
) {
}
