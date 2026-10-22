package com.lavyoung.marketforge.application.activity.model;

import java.time.LocalDateTime;

/**
 * 创建活动定义命令。
 *
 * @param activityName     活动名称
 * @param activityDesc     活动说明
 * @param beginDateTime    开始时间
 * @param endDateTime      结束时间
 * @param stockCount       库存总量
 * @param activityCountId  次数配置标识
 * @param strategyId       抽奖策略标识
 * @param awardUnlockScope 奖品解锁统计范围
 * @param operator         当前运营人员
 */
public record CreateActivityDefinitionCommand(String activityName, String activityDesc,
                                              LocalDateTime beginDateTime, LocalDateTime endDateTime,
                                              int stockCount, Long activityCountId, Long strategyId,
                                              String awardUnlockScope, String operator) {
}
