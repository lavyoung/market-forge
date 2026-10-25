package com.lavyoung.marketforge.domain.strategy.model.entity;

import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyDefinitionStateVO;

import java.util.Objects;

/**
 * 策略定义状态变更请求。
 *
 * <p>当前状态和乐观锁版本都将进入数据库更新条件，避免两个运营人员同时发布、
 * 停用同一策略时，后提交的请求覆盖已经完成的状态变化。</p>
 *
 * @param strategyId      策略业务标识
 * @param currentState    请求方读取到的当前状态
 * @param targetState     期望迁移到的目标状态
 * @param expectedVersion 请求方读取到的乐观锁版本
 * @param operator        执行状态变更的运营人员
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public record StrategyDefinitionStateChange(
        Long strategyId, StrategyDefinitionStateVO currentState,
        StrategyDefinitionStateVO targetState, int expectedVersion,
        String operator
) {

    /**
     * 创建并校验策略状态变更请求。
     *
     * @throws NullPointerException     必填字段为空时抛出
     * @throws IllegalArgumentException 标识、版本、操作人或状态迁移不合法时抛出
     */
    public StrategyDefinitionStateChange {
        Objects.requireNonNull(strategyId, "strategyId must not be null");
        Objects.requireNonNull(currentState, "currentState must not be null");
        Objects.requireNonNull(targetState, "targetState must not be null");
        Objects.requireNonNull(operator, "operator must not be null");

        if (strategyId <= 0) {
            throw new IllegalArgumentException("strategyId must be positive");
        }
        if (expectedVersion < 1) {
            throw new IllegalArgumentException("expectedVersion must be greater than zero");
        }
        if (operator.isBlank() || operator.length() > 64) {
            throw new IllegalArgumentException("operator must not be blank or exceed 64 characters");
        }
        if (!currentState.canTransitionTo(targetState)) {
            throw new IllegalArgumentException("strategy state transition is not allowed");
        }
    }
}
