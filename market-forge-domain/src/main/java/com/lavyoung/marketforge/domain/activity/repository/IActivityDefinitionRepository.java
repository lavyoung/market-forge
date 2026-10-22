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
}
