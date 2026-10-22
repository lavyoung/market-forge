package com.lavyoung.marketforge.domain.activity.model.entity;

import com.lavyoung.marketforge.domain.activity.model.vo.ActivityDefinitionStateVO;

import java.util.Objects;

/**
 * 活动定义状态变更。
 *
 * @param activityId      活动业务标识
 * @param currentState    当前状态
 * @param targetState     目标状态
 * @param expectedVersion 调用方读取到的版本
 * @param operator        操作人
 */
public record ActivityDefinitionStateChange(Long activityId, ActivityDefinitionStateVO currentState,
                                            ActivityDefinitionStateVO targetState, int expectedVersion,
                                            String operator) {
    /**
     * 创建并校验状态变更。
     *
     * @throws NullPointerException     必填字段为空时抛出
     * @throws IllegalArgumentException 标识、版本、操作人或状态迁移非法时抛出
     */
    public ActivityDefinitionStateChange {
        Objects.requireNonNull(activityId, "activityId must not be null");
        Objects.requireNonNull(currentState, "currentState must not be null");
        Objects.requireNonNull(targetState, "targetState must not be null");
        Objects.requireNonNull(operator, "operator must not be null");
        if (activityId <= 0 || expectedVersion < 1 || operator.isBlank() || operator.length() > 64) {
            throw new IllegalArgumentException("activity state change is invalid");
        }
        if (!currentState.canTransitionTo(targetState)) {
            throw new IllegalArgumentException("activity state transition is invalid");
        }
    }
}
