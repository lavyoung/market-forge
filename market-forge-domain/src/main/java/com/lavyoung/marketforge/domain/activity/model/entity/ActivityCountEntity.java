package com.lavyoung.marketforge.domain.activity.model.entity;

import lombok.Builder;

/**
 * 抽奖活动参与次数配置领域实体。
 *
 * @param activityCountId 活动参与次数配置标识
 * @param totalCount      单个用户可参与的总次数
 * @param dayCount        单个用户每日可参与次数
 * @param monthCount      单个用户每月可参与次数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Builder
public record ActivityCountEntity(
        Long activityCountId,
        Integer totalCount,
        Integer dayCount,
        Integer monthCount
) {
}
