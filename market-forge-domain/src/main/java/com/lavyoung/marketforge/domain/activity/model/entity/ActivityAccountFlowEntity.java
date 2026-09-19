package com.lavyoung.marketforge.domain.activity.model.entity;

import lombok.Builder;

/**
 * 用户抽奖活动账户流水领域实体。
 *
 * @param userId      用户标识
 * @param activityId  抽奖活动标识
 * @param totalCount  本次变动的总次数
 * @param dayCount    本次变动的日次数
 * @param monthCount  本次变动的月次数
 * @param flowId      唯一流水标识
 * @param flowChannel 流水来源渠道
 * @param bizId       外部业务标识，用于幂等校验
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Builder
public record ActivityAccountFlowEntity(
        String userId,
        Long activityId,
        Integer totalCount,
        Integer dayCount,
        Integer monthCount,
        String flowId,
        String flowChannel,
        String bizId
) {
}
