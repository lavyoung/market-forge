package com.lavyoung.marketforge.domain.activity.repository;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityDefinitionStateChange;

import java.util.Optional;

/**
 * 活动定义仓储端口。
 *
 * <p>隔离活动运营写模型与 MyBatis 技术细节，并通过条件更新守住草稿状态和乐观锁版本。</p>
 */
public interface IActivityDefinitionRepository {
    /**
     * 查询活动定义。
     *
     * @param activityId 活动标识
     * @return 匹配定义；不存在时返回空
     */
    Optional<ActivityDefinitionEntity> queryByActivityId(Long activityId);

    /**
     * 创建草稿活动定义。
     *
     * @param definition 活动定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 标识重复或写入失败时抛出
     */
    void create(ActivityDefinitionEntity definition);

    /**
     * 按草稿状态和版本更新定义。
     *
     * @param definition 最新活动定义
     * @return 更新成功返回 {@code true}
     */
    boolean updateDraft(ActivityDefinitionEntity definition);

    /**
     * 按当前状态和版本推进生命周期。
     *
     * @param stateChange 状态变更
     * @return 更新成功返回 {@code true}
     */
    boolean changeState(ActivityDefinitionStateChange stateChange);

    /**
     * 使用排他锁查询活动定义。
     *
     * <p>修改活动关联配置前必须锁定活动行，并在同一个数据库事务内完成状态校验和
     * 配置写入。活动开放操作更新同一行，因此两类操作会通过数据库行锁串行执行。</p>
     *
     * @param activityId 活动业务标识
     * @return 已锁定的活动定义；活动不存在时返回空
     * @throws NullPointerException 活动标识为空时抛出
     */
    Optional<ActivityDefinitionEntity> lockByActivityId(Long activityId);
}
