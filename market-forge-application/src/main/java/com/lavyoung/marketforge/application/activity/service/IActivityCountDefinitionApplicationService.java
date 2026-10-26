package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityCountDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.CreateActivityCountDefinitionCommand;
import com.lavyoung.marketforge.application.activity.model.UpdateActivityCountDefinitionCommand;

/**
 * 活动次数配置运营应用服务。
 *
 * <p>负责编排业务标识生成、领域规则校验、开放活动引用保护和乐观锁更新。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public interface IActivityCountDefinitionApplicationService {

    /**
     * 创建活动次数配置。
     *
     * @param command 创建命令
     * @return 创建后的活动次数配置
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 配置非法、重复或写入失败时抛出
     */
    ActivityCountDefinitionResult create(CreateActivityCountDefinitionCommand command);

    /**
     * 查询活动次数配置详情。
     *
     * @param activityCountId 活动次数配置业务标识
     * @return 活动次数配置详情
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 配置不存在时抛出
     */
    ActivityCountDefinitionResult queryByActivityCountId(Long activityCountId);

    /**
     * 修改未被开放活动引用的次数配置。
     *
     * @param command 修改命令
     * @return 修改后的活动次数配置
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 配置非法、已被引用或版本冲突时抛出
     */
    ActivityCountDefinitionResult update(UpdateActivityCountDefinitionCommand command);
}
