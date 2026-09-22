package com.lavyoung.marketforge.domain.activity.model.entity;

import lombok.Builder;

import java.time.LocalDate;

/**
 * 用户活动日账户领域实体。
 *
 * @param userId          用户标识
 * @param activityId      活动标识
 * @param day             账户归属日期
 * @param dayCount        当日可用次数
 * @param dayCountSurplus 当日剩余次数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Builder
public record ActivityAccountDayEntity(
        String userId,
        Long activityId,
        LocalDate day,
        Integer dayCount,
        Integer dayCountSurplus
) {
}
