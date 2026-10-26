package com.lavyoung.marketforge.domain.activity.repository;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountDefinitionEntity;

import java.util.Optional;

/**
 * 活动次数配置运营仓储端口。
 *
 * <p>该仓储负责活动次数配置的创建、详情读取和受保护更新。
 * MyBatis、PO、SQL 和数据库异常转换由基础设施层负责，不得泄漏到领域层。</p>
 *
 * <p>抽奖运行时读取次数配置仍然通过 {@link IActivityRepository} 完成。
 * 本仓储只承担运营写模型职责，避免后台管理逻辑进入高频抽奖链路。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public interface IActivityCountDefinitionRepository {

    /**
     * 根据业务标识查询活动次数配置定义。
     *
     * <p>该方法返回包含名称、乐观锁版本和审计字段的运营定义，
     * 不使用只包含三个额度字段的运行时模型。</p>
     *
     * @param activityCountId 活动次数配置业务标识
     * @return 匹配的运营配置；不存在时返回空
     * @throws NullPointerException 活动次数配置标识为空时抛出
     */
    Optional<ActivityCountDefinitionEntity> queryByActivityCountId(Long activityCountId);

    /**
     * 使用排他锁读取活动次数配置。
     *
     * <p>活动从草稿切换为开放状态时必须先锁定对应配置行，并将锁持有到活动状态事务提交。
     * 次数配置更新同样会锁定该行，因此开放活动与修改配置会按数据库行锁顺序串行执行。</p>
     *
     * @param activityCountId 活动次数配置业务标识
     * @return 已锁定的次数配置；配置不存在时返回空
     * @throws NullPointerException 活动次数配置标识为空时抛出
     */
    Optional<ActivityCountDefinitionEntity> lockByActivityCountId(Long activityCountId);

    /**
     * 创建活动次数配置定义。
     *
     * <p>实现方应将业务标识、配置名称、总/月/日次数、初始版本和审计信息
     * 一次性写入数据库。业务标识重复时，需要把数据库唯一键异常转换为稳定的
     * {@code BusinessException}，不得直接向应用层暴露 {@code DuplicateKeyException}。</p>
     *
     * @param definition 待创建的活动次数配置
     * @throws NullPointerException                                       配置定义为空时抛出
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 配置重复或持久化失败时抛出
     */
    void create(ActivityCountDefinitionEntity definition);

    /**
     * 判断次数配置是否被开放活动引用。
     *
     * <p>只统计状态为 {@code open} 的活动。草稿活动仍允许运营人员调整次数配置，
     * 已关闭活动不会继续接受新用户参与，因此也不会阻止修改。</p>
     *
     * <p>该查询用于在执行更新前返回明确的业务错误，但不能单独作为并发保护。
     * 最终更新 SQL 仍必须再次包含“不存在开放活动引用”的条件。</p>
     *
     * @param activityCountId 活动次数配置业务标识
     * @return 存在开放活动引用时返回 {@code true}
     * @throws NullPointerException 活动次数配置标识为空时抛出
     */
    boolean existsOpenActivityReference(Long activityCountId);

    /**
     * 按乐观锁版本更新未被开放活动引用的次数配置。
     *
     * <p>实现方必须使用一条条件更新 SQL，同时校验：</p>
     *
     * <ol>
     *     <li>业务标识等于 {@code activityCountId}。</li>
     *     <li>数据库版本等于实体携带的 {@code version}。</li>
     *     <li>不存在状态为 {@code open} 且引用当前次数配置的活动。</li>
     * </ol>
     *
     * <p>更新成功后数据库版本必须原子增加一。不能先查询引用关系，再执行一条
     * 不带引用条件的普通更新，否则活动可能在两条 SQL 之间开放，导致运行中的
     * 活动配置被修改。</p>
     *
     * @param definition 包含最新配置和期望版本的活动次数定义
     * @return 更新成功返回 {@code true}；版本冲突、配置不存在或已被开放活动引用时返回 {@code false}
     * @throws NullPointerException 配置定义为空时抛出
     */
    boolean updateIfNotReferencedByOpenActivity(ActivityCountDefinitionEntity definition);
}
