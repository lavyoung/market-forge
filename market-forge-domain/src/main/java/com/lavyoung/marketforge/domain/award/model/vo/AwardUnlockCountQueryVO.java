package com.lavyoung.marketforge.domain.award.model.vo;

import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 奖品解锁次数查询条件。
 *
 * <p>该值对象用于查询用户在指定活动中已经完成的有效抽奖次数。
 * 统计范围由活动配置决定，不允许仓储实现自行选择日次数或累计次数。</p>
 *
 * <p>当范围为 {@link AwardUnlockScopeVO#DAY} 时，只统计
 * {@code statisticsDate} 当天产生的中奖记录；当范围为
 * {@link AwardUnlockScopeVO#TOTAL} 时，统计当前活动的全部历史中奖记录。</p>
 *
 * @param userId         用户标识，同时也是中奖记录的分片键
 * @param activityId     活动标识，用于隔离不同活动的抽奖记录
 * @param unlockScope    活动配置的奖品解锁次数统计范围
 * @param statisticsDate 日范围统计所使用的业务日期；累计统计时保留该值以统一查询契约
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/17
 */
public record AwardUnlockCountQueryVO(
        String userId,
        Long activityId,
        AwardUnlockScopeVO unlockScope,
        LocalDate statisticsDate
) {

    /**
     * 校验奖品解锁次数查询条件。
     *
     * @throws IllegalArgumentException 用户标识为空白或活动标识不是正数时抛出
     * @throws NullPointerException     活动标识、统计范围或业务日期为空时抛出
     */
    public AwardUnlockCountQueryVO {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId must not be blank");
        }

        Objects.requireNonNull(activityId, "activityId must not be null");
        if (activityId <= 0) {
            throw new IllegalArgumentException("activityId must be positive");
        }

        Objects.requireNonNull(unlockScope, "unlockScope must not be null");
        Objects.requireNonNull(statisticsDate, "statisticsDate must not be null");
    }
}
