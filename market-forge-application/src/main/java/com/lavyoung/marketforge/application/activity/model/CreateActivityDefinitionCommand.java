package com.lavyoung.marketforge.application.activity.model;

import java.time.LocalDateTime;

/**
 * 创建活动定义命令。
 *
 * <p>该命令只携带活动级配置，不包含库存。活动参与额度的可售数量由活动 SKU
 * 单独配置，策略奖品的可中奖数量由策略奖品配置负责。</p>
 *
 * @param activityName     活动名称
 * @param activityDesc     活动说明
 * @param beginDateTime    活动开始时间
 * @param endDateTime      活动结束时间
 * @param activityCountId  活动使用的次数配置标识
 * @param strategyId       活动使用的抽奖策略标识
 * @param awardUnlockScope 奖品解锁统计范围
 * @param operator         当前运营人员
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/28
 */
public record CreateActivityDefinitionCommand(
        String activityName,
        String activityDesc,
        LocalDateTime beginDateTime,
        LocalDateTime endDateTime,
        Long activityCountId,
        Long strategyId,
        String awardUnlockScope,
        String operator
) {
}
