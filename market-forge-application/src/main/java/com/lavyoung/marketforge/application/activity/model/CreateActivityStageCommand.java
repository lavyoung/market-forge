package com.lavyoung.marketforge.application.activity.model;

/**
 * 创建活动展台命令。
 *
 * @param channel    投放渠道
 * @param source     流量来源
 * @param activityId 待上架的活动标识
 * @param operator   当前运营人员标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public record CreateActivityStageCommand(String channel, String source, Long activityId, String operator) {
}
