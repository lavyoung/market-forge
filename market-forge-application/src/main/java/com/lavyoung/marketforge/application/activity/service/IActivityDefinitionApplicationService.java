package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ChangeActivityDefinitionStateCommand;
import com.lavyoung.marketforge.application.activity.model.CreateActivityDefinitionCommand;
import com.lavyoung.marketforge.application.activity.model.UpdateActivityDefinitionCommand;

/**
 * 活动定义运营写用例。
 */
public interface IActivityDefinitionApplicationService {
    /**
     * 创建活动草稿。
     *
     * @param command 创建命令
     * @return 创建后的活动定义
     */
    ActivityDefinitionResult create(CreateActivityDefinitionCommand command);

    /**
     * 查询活动最新详情。
     *
     * @param activityId 活动标识
     * @return 活动定义
     */
    ActivityDefinitionResult queryByActivityId(Long activityId);

    /**
     * 修改活动草稿。
     *
     * @param command 修改命令
     * @return 修改后的活动定义
     */
    ActivityDefinitionResult updateDraft(UpdateActivityDefinitionCommand command);

    /**
     * 推进活动生命周期。
     *
     * @param command 状态变更命令
     * @return 状态变更后的活动定义
     */
    ActivityDefinitionResult changeState(ChangeActivityDefinitionStateCommand command);
}
