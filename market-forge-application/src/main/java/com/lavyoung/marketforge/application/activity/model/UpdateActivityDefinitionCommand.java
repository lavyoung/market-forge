package com.lavyoung.marketforge.application.activity.model;

import java.time.LocalDateTime;

/**
 * 修改活动草稿命令。
 *
 * @param activityId       活动标识
 * @param activityName     活动名称
 * @param activityDesc     活动说明
 * @param beginDateTime    开始时间
 * @param endDateTime      结束时间
 * @param stockCount       库存总量
 * @param activityCountId  次数配置标识
 * @param strategyId       抽奖策略标识
 * @param awardUnlockScope 奖品解锁统计范围
 * @param expectedVersion  页面读取到的版本
 * @param operator         当前运营人员
 */
public record UpdateActivityDefinitionCommand(Long activityId, String activityName, String activityDesc,
                                              LocalDateTime beginDateTime, LocalDateTime endDateTime,
                                              int stockCount, Long activityCountId, Long strategyId,
                                              String awardUnlockScope, int expectedVersion, String operator) {
}
