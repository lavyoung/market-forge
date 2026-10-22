package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityStageResult;
import com.lavyoung.marketforge.application.activity.model.ActivityStageSearchQuery;
import com.lavyoung.marketforge.types.model.PageResult;

/**
 * 活动展台运营查询应用服务。
 *
 * <p>为 Trigger 层提供稳定的分页查询用例，写入和发布仍由
 * {@link IActivityStageApplicationService} 负责。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
public interface IActivityStageSearchService {

    /**
     * 分页查询活动展台。
     *
     * @param query 活动展台查询条件
     * @return 活动展台分页结果
     * @throws NullPointerException 查询条件为空时抛出
     * @throws RuntimeException     读取端口执行失败时抛出
     */
    PageResult<ActivityStageResult> search(ActivityStageSearchQuery query);
}
