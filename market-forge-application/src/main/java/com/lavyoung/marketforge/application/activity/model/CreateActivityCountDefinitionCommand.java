package com.lavyoung.marketforge.application.activity.model;

/**
 * 创建活动次数配置命令。
 *
 * @param activityCountName 配置名称
 * @param totalCount        一次入账增加的总抽奖次数
 * @param dayCount          一次入账增加的日抽奖次数
 * @param monthCount        一次入账增加的月抽奖次数
 * @param operator          当前运营人员标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public record CreateActivityCountDefinitionCommand(String activityCountName, int totalCount, int dayCount,
                                                   int monthCount, String operator) {
}
