package com.lavyoung.marketforge.application.activity.model;

/**
 * 修改活动次数配置命令。
 *
 * @param activityCountId   活动次数配置业务标识
 * @param activityCountName 最新配置名称
 * @param totalCount        最新总入账次数
 * @param dayCount          最新日入账次数
 * @param monthCount        最新月入账次数
 * @param expectedVersion   调用方读取到的乐观锁版本
 * @param operator          当前运营人员标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public record UpdateActivityCountDefinitionCommand(Long activityCountId, String activityCountName, int totalCount,
                                                   int dayCount, int monthCount, int expectedVersion, String operator) {
}
