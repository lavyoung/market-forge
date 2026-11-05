package com.lavyoung.marketforge.domain.activity.repository;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStagePublicationEntity;

import java.util.Optional;

/**
 * 活动展台发布记录仓储端口。
 *
 * <p>负责发布任务的幂等创建、按展台版本查询和条件状态迁移。
 * MyBatis、唯一键冲突和条件更新 SQL 由 Infrastructure 实现，
 * 不向 Application 或 Domain 泄漏持久化技术类型。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/05
 */
public interface IActivityStagePublicationRepository {

    /**
     * 查询指定展台版本对应的发布记录。
     *
     * @param stageId      展台业务标识
     * @param stageVersion 展台乐观锁版本
     * @return 已存在的发布记录；尚未发布时返回空
     */
    Optional<ActivityStagePublicationEntity> queryByStageAndVersion(Long stageId, int stageVersion);

    /**
     * 幂等创建发布记录。
     *
     * <p>数据库必须对 {@code stage_id + stage_version} 建立唯一约束。
     * 并发请求中只有一个请求可以创建成功，其他请求返回 {@code false}，
     * 随后应查询并复用已有发布记录。</p>
     *
     * @param publication 首次发布记录
     * @return 创建成功返回 {@code true}；相同展台版本已经存在时返回 {@code false}
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 其他数据库写入错误时抛出
     */
    boolean createIfAbsent(ActivityStagePublicationEntity publication);

    /**
     * 使用当前状态、阶段和尝试次数作为条件更新发布进度。
     *
     * <p>实现必须同时匹配发布标识、当前状态、当前阶段和当前尝试次数，
     * 防止两个并发重试请求覆盖彼此的执行结果。</p>
     *
     * @param current 更新前的发布记录快照
     * @param target  通过领域行为生成的目标发布记录
     * @return 条件更新成功返回 {@code true}；快照已过期时返回 {@code false}
     */
    boolean transition(ActivityStagePublicationEntity current, ActivityStagePublicationEntity target);
}
