package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionSearchQuery;
import com.lavyoung.marketforge.types.model.PageResult;

/**
 * 活动定义运营查询应用服务。
 *
 * <p>为 Trigger 层提供稳定的活动定义分页查询用例。HTTP 请求必须先转换成
 * {@link ActivityDefinitionSearchQuery}，不得把 API Request 直接传入应用层。</p>
 *
 * <p>该服务只处理读取用例，不负责创建、编辑、开放或关闭活动。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public interface IActivityDefinitionSearchService {

    /**
     * 分页查询活动定义。
     *
     * @param query 活动定义查询条件
     * @return 活动定义分页结果
     * @throws NullPointerException 查询条件为空时抛出
     * @throws RuntimeException     读取活动定义失败时抛出
     */
    PageResult<ActivityDefinitionResult> search(ActivityDefinitionSearchQuery query);
}
