package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityCountDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivityCountDefinitionSearchQuery;
import com.lavyoung.marketforge.types.model.PageResult;

/**
 * 活动次数配置运营查询应用服务。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public interface IActivityCountDefinitionSearchService {

    /**
     * 分页查询活动次数配置。
     *
     * @param query 查询条件
     * @return 活动次数配置分页结果
     * @throws NullPointerException 查询条件为空时抛出
     */
    PageResult<ActivityCountDefinitionResult> search(ActivityCountDefinitionSearchQuery query);
}
