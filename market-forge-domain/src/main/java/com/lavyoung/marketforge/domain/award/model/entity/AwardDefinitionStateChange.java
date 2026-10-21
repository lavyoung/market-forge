package com.lavyoung.marketforge.domain.award.model.entity;

import com.lavyoung.marketforge.domain.award.model.vo.AwardDefinitionStateVO;

import java.util.Objects;

/**
 * 奖品定义状态变更请求。
 *
 * <p>当前状态和乐观锁版本将作为数据库更新条件，防止两个运营人员同时操作时，
 * 后提交的请求覆盖已经发生的状态变化。</p>
 *
 * @param awardId         奖品业务标识
 * @param currentState    请求方读取到的当前状态
 * @param targetState     期望变更到的目标状态
 * @param expectedVersion 请求方读取到的乐观锁版本
 * @param operator        执行状态变更的运营人员标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
public record AwardDefinitionStateChange(
        Long awardId,
        AwardDefinitionStateVO currentState,
        AwardDefinitionStateVO targetState,
        int expectedVersion,
        String operator
) {
    /**
     * 创建并校验奖品定义状态变更请求。
     *
     * @throws NullPointerException     必填字段为空时抛出
     * @throws IllegalArgumentException 奖品标识、版本、操作人或状态变化不合法时抛出
     */
    public AwardDefinitionStateChange {
        Objects.requireNonNull(awardId, "awardId must not be null");
        Objects.requireNonNull(currentState, "currentState must not be null");
        Objects.requireNonNull(targetState, "targetState must not be null");
        Objects.requireNonNull(operator, "operator must not be null");

        if (awardId <= 0) {
            throw new IllegalArgumentException("awardId must be positive");
        }
        if (currentState == targetState) {
            throw new IllegalArgumentException("targetState must be different from currentState");
        }
        if (expectedVersion < 1) {
            throw new IllegalArgumentException("expectedVersion must be greater than zero");
        }
        if (operator.isBlank()) {
            throw new IllegalArgumentException("operator must not be blank");
        }
        if (operator.length() > 64) {
            throw new IllegalArgumentException("operator length must not exceed 64");
        }
    }
}
