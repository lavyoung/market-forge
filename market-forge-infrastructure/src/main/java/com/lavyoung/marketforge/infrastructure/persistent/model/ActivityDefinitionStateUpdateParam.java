package com.lavyoung.marketforge.infrastructure.persistent.model;

/**
 * 活动定义状态条件更新参数。
 *
 * @param activityId      活动标识
 * @param currentState    当前状态
 * @param targetState     目标状态
 * @param expectedVersion 期望版本
 * @param operator        操作人
 */
public record ActivityDefinitionStateUpdateParam(Long activityId, String currentState, String targetState,
                                                 int expectedVersion, String operator) {
}
