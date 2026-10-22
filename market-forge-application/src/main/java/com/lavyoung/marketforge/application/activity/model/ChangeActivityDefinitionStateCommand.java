package com.lavyoung.marketforge.application.activity.model;

/**
 * 变更活动定义状态命令。
 *
 * @param activityId      活动标识
 * @param targetState     目标状态
 * @param expectedVersion 页面读取到的版本
 * @param operator        当前运营人员
 */
public record ChangeActivityDefinitionStateCommand(Long activityId, String targetState,
                                                   int expectedVersion, String operator) {
}
