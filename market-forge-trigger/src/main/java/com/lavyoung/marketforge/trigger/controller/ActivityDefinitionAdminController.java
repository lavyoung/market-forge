package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.activity.IActivityDefinitionAdminApi;
import com.lavyoung.marketforge.api.activity.request.ActivityDefinitionSearchRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityDefinitionResponse;
import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionSearchQuery;
import com.lavyoung.marketforge.application.activity.service.IActivityDefinitionSearchService;
import com.lavyoung.marketforge.trigger.assembler.ActivityDefinitionApiAssembler;
import com.lavyoung.marketforge.types.model.PageResult;
import com.lavyoung.marketforge.types.model.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * 活动定义运营查询 HTTP 适配器。
 *
 * <p>只负责 HTTP Request、Application Query 与 API Response 之间的协议适配。
 * 数据库访问由 Infrastructure Reader 完成，Controller 不依赖 DAO、Repository
 * 或领域服务。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@RestController
@RequiredArgsConstructor
public class ActivityDefinitionAdminController implements IActivityDefinitionAdminApi {

    private final IActivityDefinitionSearchService activityDefinitionSearchService;
    private final ActivityDefinitionApiAssembler activityDefinitionApiAssembler;

    @Override
    public Response<PageResult<ActivityDefinitionResponse>> searchActivityDefinitions(ActivityDefinitionSearchRequest request) {
        ActivityDefinitionSearchQuery query = activityDefinitionApiAssembler.toSearchQuery(request);
        PageResult<ActivityDefinitionResult> result = activityDefinitionSearchService.search(query);
        return Response.success(activityDefinitionApiAssembler.toPageResponse(result));
    }
}
