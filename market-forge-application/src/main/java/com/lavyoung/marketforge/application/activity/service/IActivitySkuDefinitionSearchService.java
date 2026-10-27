package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivitySkuDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivitySkuDefinitionSearchQuery;
import com.lavyoung.marketforge.types.model.PageResult;

/**
 * 活动 SKU 运营查询应用服务。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
public interface IActivitySkuDefinitionSearchService {

    /**
     * 分页查询活动 SKU。
     *
     * @param query 查询条件
     * @return 活动 SKU 分页结果
     * @throws NullPointerException 查询条件为空时抛出
     */
    PageResult<ActivitySkuDefinitionResult> search(ActivitySkuDefinitionSearchQuery query);
}
