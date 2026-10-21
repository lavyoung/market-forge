package com.lavyoung.marketforge.infrastructure.persistent.model;

/**
 * 奖品定义状态条件更新参数。
 *
 * <p>该对象是 MyBatis 更新参数，不属于领域模型。它将领域状态转换为数据库字符串编码，
 * 并携带当前状态和预期版本，用于实现单条 SQL 的并发安全状态更新。</p>
 *
 * @param awardId         奖品业务标识
 * @param currentState    当前状态编码，作为更新条件
 * @param targetState     目标状态编码
 * @param expectedVersion 调用方读取到的乐观锁版本
 * @param operator        执行状态变更的运营人员标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
public record AwardDefinitionStateUpdateParam(
        Long awardId,
        String currentState,
        String targetState,
        int expectedVersion,
        String operator
) {
}
