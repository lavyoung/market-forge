package com.lavyoung.marketforge.application.activity.model;

import java.time.LocalDateTime;

/**
 * 修改活动草稿命令。
 *
 * <p>活动草稿允许修改活动名称、有效期、次数配置、抽奖策略和奖品解锁范围。
 * 库存不属于活动定义，因此不通过该命令修改。</p>
 *
 * @param activityId       活动标识
 * @param activityName     活动名称
 * @param activityDesc     活动说明
 * @param beginDateTime    活动开始时间
 * @param endDateTime      活动结束时间
 * @param activityCountId  活动使用的次数配置标识
 * @param strategyId       活动使用的抽奖策略标识
 * @param awardUnlockScope 奖品解锁统计范围
 * @param expectedVersion  页面读取到的乐观锁版本
 * @param operator         当前运营人员
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/28
 */
public record UpdateActivityDefinitionCommand(
        Long activityId,
        String activityName,
        String activityDesc,
        LocalDateTime beginDateTime,
        LocalDateTime endDateTime,
        Long activityCountId,
        Long strategyId,
        String awardUnlockScope,
        int expectedVersion,
        String operator
) {
}
