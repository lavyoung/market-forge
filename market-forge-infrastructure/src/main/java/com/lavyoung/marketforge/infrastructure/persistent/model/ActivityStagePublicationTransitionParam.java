package com.lavyoung.marketforge.infrastructure.persistent.model;

import java.time.LocalDateTime;

/**
 * 活动展台发布记录条件迁移参数。
 *
 * <p>更新发布进度时，同时匹配发布标识、原状态、原阶段和原尝试次数。
 * 这些字段共同构成发布记录的并发快照，任何一个字段已经变化，都说明当前请求
 * 持有的发布记录已经过期，SQL 必须拒绝覆盖最新执行结果。</p>
 *
 * <p>目标字段来自经过领域实体校验的新快照。Infrastructure 只负责把合法状态
 * 写入数据库，不在 SQL 或 Repository 中重新实现领域状态机。</p>
 *
 * @param publicationId        发布任务业务标识
 * @param expectedState        更新前预期的发布状态编码
 * @param expectedPhase        更新前预期的发布阶段编码
 * @param expectedAttemptCount 更新前预期的发布尝试次数
 * @param targetState          更新后的发布状态编码
 * @param targetPhase          更新后的发布阶段编码
 * @param targetAttemptCount   更新后的发布尝试次数
 * @param operator             最近一次执行发布操作的运营人员
 * @param failureCode          最近一次失败的稳定错误码
 * @param failureReason        最近一次失败的安全说明
 * @param publishedAt          发布成功时间
 * @param updateTime           本次状态变化时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/05
 */
public record ActivityStagePublicationTransitionParam(
        Long publicationId,
        String expectedState,
        String expectedPhase,
        int expectedAttemptCount,
        String targetState,
        String targetPhase,
        int targetAttemptCount,
        String operator,
        String failureCode,
        String failureReason,
        LocalDateTime publishedAt,
        LocalDateTime updateTime
) {
}
