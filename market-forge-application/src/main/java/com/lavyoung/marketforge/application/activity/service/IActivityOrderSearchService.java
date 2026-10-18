package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityOrderSearchItem;
import com.lavyoung.marketforge.application.activity.model.ActivityOrderSearchQuery;
import com.lavyoung.marketforge.types.model.PageResult;

/**
 * 活动订单运营查询应用服务。
 *
 * <p>该服务向 Trigger 层提供稳定的订单查询用例。Trigger 层应先将 HTTP
 * 请求转换为 {@link ActivityOrderSearchQuery}，不得将 API 请求对象直接传入应用层。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/18
 */
public interface IActivityOrderSearchService {

    /**
     * 分页检索活动订单。
     *
     * @param query 活动订单查询条件
     * @return 满足条件的活动订单分页结果
     * @throws NullPointerException 查询条件为空时抛出
     * @throws RuntimeException     搜索服务不可用或查询失败时抛出
     */
    PageResult<ActivityOrderSearchItem> search(ActivityOrderSearchQuery query);
}
