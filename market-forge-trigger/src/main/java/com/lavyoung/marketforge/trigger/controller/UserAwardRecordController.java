package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.award.IUserAwardRecordApi;
import com.lavyoung.marketforge.api.award.request.UserAwardRecordSearchRequest;
import com.lavyoung.marketforge.api.award.response.UserAwardRecordResponse;
import com.lavyoung.marketforge.application.award.model.UserAwardRecordSearchItem;
import com.lavyoung.marketforge.application.award.model.UserAwardRecordSearchQuery;
import com.lavyoung.marketforge.application.award.service.IUserAwardRecordSearchService;
import com.lavyoung.marketforge.trigger.assembler.UserAwardRecordSearchAssembler;
import com.lavyoung.marketforge.types.model.PageResult;
import com.lavyoung.marketforge.types.model.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户中奖记录运营查询 HTTP 适配器。
 *
 * <p>该控制器实现用户中奖记录 REST API 契约，将 HTTP 查询请求转换为
 * Application 查询条件，调用中奖记录搜索应用服务，并将应用查询结果转换为
 * 对外稳定的分页响应。</p>
 *
 * <p>该控制器不直接依赖 Elasticsearch 客户端，不构造搜索 DSL，
 * 也不处理 Canal 数据同步。搜索实现和索引细节由 Infrastructure 层负责。</p>
 *
 * <p>由于 MySQL 数据通过 Canal 异步同步到 Elasticsearch，
 * 当前接口提供的是最终一致性的运营查询结果，不应被用于强一致性的发奖决策。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/16
 */
@RestController
@RequiredArgsConstructor
public class UserAwardRecordController implements IUserAwardRecordApi {

    private final IUserAwardRecordSearchService userAwardRecordSearchService;
    private final UserAwardRecordSearchAssembler userAwardRecordSearchAssembler;

    @Override
    public Response<PageResult<UserAwardRecordResponse>> searchUserAwardRecords(UserAwardRecordSearchRequest request) {
        UserAwardRecordSearchQuery query = userAwardRecordSearchAssembler.toQuery(request);
        PageResult<UserAwardRecordSearchItem> pageResult = userAwardRecordSearchService.search(query);
        return Response.success(userAwardRecordSearchAssembler.toPageResponse(pageResult));
    }
}
