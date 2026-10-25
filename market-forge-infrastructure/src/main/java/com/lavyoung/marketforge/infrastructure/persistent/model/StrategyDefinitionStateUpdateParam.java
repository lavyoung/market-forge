package com.lavyoung.marketforge.infrastructure.persistent.model;

/**
 * 策略定义状态条件更新参数。
 *
 * <p>该对象属于 MyBatis 持久化边界，不是领域模型。它把领域生命周期状态转换为
 * 数据库字符串编码，并携带当前状态和乐观锁版本，用于完成单条原子更新。</p>
 *
 * @param strategyId      策略业务标识
 * @param currentState    当前状态编码，参与更新条件
 * @param targetState     目标状态编码
 * @param expectedVersion 调用方读取到的乐观锁版本
 * @param operator        执行状态变更的运营人员
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public record StrategyDefinitionStateUpdateParam(
        Long strategyId, String currentState, String targetState,
        int expectedVersion, String operator
) {
}
