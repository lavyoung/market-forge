package com.lavyoung.marketforge.application.activity.model;

/**
 * 修改待上架活动展台命令。
 *
 * @param stageId         展台业务标识
 * @param channel         最新投放渠道
 * @param source          最新流量来源
 * @param activityId      最新关联活动标识
 * @param expectedVersion 调用方读取到的乐观锁版本
 * @param operator        当前运营人员标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public record UpdateActivityStageCommand(Long stageId, String channel, String source, Long activityId,
                                         int expectedVersion, String operator) {
}
