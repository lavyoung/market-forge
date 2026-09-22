package com.lavyoung.marketforge.domain.activity.model.entity;

import lombok.Builder;

import java.time.YearMonth;

/**
 * 用户活动月账户领域实体。
 *
 * @param userId            用户标识
 * @param activityId        活动标识
 * @param month             账户归属月份
 * @param monthCount        当月可用次数
 * @param monthCountSurplus 当月剩余次数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Builder
public record ActivityAccountMonthEntity(
        String userId,
        Long activityId,
        YearMonth month,
        Integer monthCount,
        Integer monthCountSurplus
) {
}
