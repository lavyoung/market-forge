package com.lavyoung.marketforge.infrastructure.persistent.model;

/**
 * 活动展台状态条件更新参数。
 *
 * @param stageId         展台业务标识
 * @param currentState    期望的当前状态编码
 * @param targetState     目标状态编码
 * @param expectedVersion 期望的乐观锁版本
 * @param operator        操作人标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public record ActivityStageStateUpdateParam(Long stageId, String currentState, String targetState,
                                            int expectedVersion, String operator) {
}
