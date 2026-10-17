package com.lavyoung.marketforge.domain.activity.model.entity;

import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 抽奖活动领域实体。
 *
 * @param activityId        抽奖活动标识
 * @param activityName      活动名称
 * @param activityDesc      活动说明
 * @param beginDateTime     活动开始时间
 * @param endDateTime       活动结束时间
 * @param stockCount        活动库存总量
 * @param stockCountSurplus 活动剩余库存
 * @param activityCountId   活动参与次数配置标识
 * @param strategyId        活动关联的抽奖策略标识
 * @param awardUnlockScope  奖品次数锁的统计范围
 * @param state             活动状态
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Builder
public record ActivityEntity(
        Long activityId,
        String activityName,
        String activityDesc,
        LocalDateTime beginDateTime,
        LocalDateTime endDateTime,
        Integer stockCount,
        Integer stockCountSurplus,
        Long activityCountId,
        Long strategyId,
        AwardUnlockScopeVO awardUnlockScope,
        String state
) {
}
