package com.lavyoung.marketforge.domain.activity.model.entity;

import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStageStateVO;

import java.util.Objects;

/**
 * 活动展台状态变更。
 *
 * <p>同时携带当前状态和乐观锁版本，使基础设施可以通过条件更新保证状态流转不会覆盖
 * 其他运营人员刚刚完成的发布操作。</p>
 *
 * @param stageId         展台业务标识
 * @param currentState    调用方期望的当前状态
 * @param targetState     需要推进到的目标状态
 * @param expectedVersion 调用方读取到的乐观锁版本
 * @param operator        执行状态变更的运营人员标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public record ActivityStageStateChange(Long stageId, ActivityStageStateVO currentState,
                                       ActivityStageStateVO targetState,
                                       int expectedVersion, String operator) {

    /**
     * 创建并校验展台状态变更参数。
     *
     * @throws NullPointerException     必填参数为空时抛出
     * @throws IllegalArgumentException 标识、版本、操作人或状态流转不合法时抛出
     */
    public ActivityStageStateChange {
        Objects.requireNonNull(stageId, "stageId must not be null");
        Objects.requireNonNull(currentState, "currentState must not be null");
        Objects.requireNonNull(targetState, "targetState must not be null");
        Objects.requireNonNull(operator, "operator must not be null");
        if (stageId <= 0) {
            throw new IllegalArgumentException("stageId must be positive");
        }
        if (expectedVersion < 1) {
            throw new IllegalArgumentException("expectedVersion must be greater than zero");
        }
        if (operator.isBlank() || operator.length() > 64) {
            throw new IllegalArgumentException("operator must contain 1 to 64 characters");
        }
        if (!currentState.canTransitionTo(targetState)) {
            throw new IllegalArgumentException("activity stage state transition is invalid");
        }
    }
}
