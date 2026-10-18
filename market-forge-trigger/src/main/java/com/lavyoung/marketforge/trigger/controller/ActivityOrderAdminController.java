package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.activity.IActivityOrderAdminApi;
import com.lavyoung.marketforge.api.activity.request.ActivityOrderSearchRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityOrderResponse;
import com.lavyoung.marketforge.application.activity.model.ActivityOrderSearchItem;
import com.lavyoung.marketforge.application.activity.model.ActivityOrderSearchQuery;
import com.lavyoung.marketforge.application.activity.service.IActivityOrderSearchService;
import com.lavyoung.marketforge.trigger.assembler.ActivityOrderSearchAssembler;
import com.lavyoung.marketforge.types.model.PageResult;
import com.lavyoung.marketforge.types.model.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * 活动订单运营查询 HTTP 适配器。
 *
 * <p>该控制器实现运营活动订单 REST API 契约，将 HTTP 查询参数转换为
 * Application 查询条件，调用活动订单搜索服务，并将应用结果转换为稳定的
 * API 分页响应。</p>
 *
 * <p>该控制器不构造 Elasticsearch DSL，不访问数据库或缓存，也不参与订单状态
 * 流转。搜索实现与索引细节全部由 Infrastructure 层负责。</p>
 *
 * <p>由于订单数据通过 Canal 从 MySQL 异步同步到 Elasticsearch，
 * 接口提供最终一致性的运营查询结果，不能用于强一致性的交易决策。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/18
 */
@RestController
@RequiredArgsConstructor
public class ActivityOrderAdminController implements IActivityOrderAdminApi {

    private final IActivityOrderSearchService activityOrderSearchService;
    private final ActivityOrderSearchAssembler activityOrderSearchAssembler;

    /**
     * {@inheritDoc}
     */
    @Override
    public Response<PageResult<ActivityOrderResponse>> searchActivityOrders(ActivityOrderSearchRequest request) {
        ActivityOrderSearchQuery query = activityOrderSearchAssembler.toQuery(request);
        PageResult<ActivityOrderSearchItem> pageResult = activityOrderSearchService.search(query);
        PageResult<ActivityOrderResponse> responsePage = activityOrderSearchAssembler.toPageResponse(pageResult);
        return Response.success(responsePage);
    }
}
