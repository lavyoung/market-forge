package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.activity.IActivityCountDefinitionAdminApi;
import com.lavyoung.marketforge.api.activity.request.ActivityCountDefinitionSearchRequest;
import com.lavyoung.marketforge.api.activity.request.CreateActivityCountDefinitionRequest;
import com.lavyoung.marketforge.api.activity.request.UpdateActivityCountDefinitionRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityCountDefinitionResponse;
import com.lavyoung.marketforge.application.activity.model.ActivityCountDefinitionSearchQuery;
import com.lavyoung.marketforge.application.activity.model.CreateActivityCountDefinitionCommand;
import com.lavyoung.marketforge.application.activity.model.UpdateActivityCountDefinitionCommand;
import com.lavyoung.marketforge.application.activity.service.IActivityCountDefinitionApplicationService;
import com.lavyoung.marketforge.application.activity.service.IActivityCountDefinitionSearchService;
import com.lavyoung.marketforge.trigger.assembler.ActivityCountDefinitionApiAssembler;
import com.lavyoung.marketforge.trigger.security.AdminSessionManager;
import com.lavyoung.marketforge.types.model.PageResult;
import com.lavyoung.marketforge.types.model.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * 活动次数配置运营管理 HTTP 适配器。
 *
 * <p>Controller 只负责协议转换、认证身份提取和应用服务调用，不承载次数关系、
 * 引用保护或乐观锁规则。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
@RestController
@RequiredArgsConstructor
public class ActivityCountDefinitionAdminController implements IActivityCountDefinitionAdminApi {

    private final IActivityCountDefinitionApplicationService activityCountDefinitionApplicationService;
    private final IActivityCountDefinitionSearchService activityCountDefinitionSearchService;
    private final ActivityCountDefinitionApiAssembler activityCountDefinitionApiAssembler;
    private final AdminSessionManager adminSessionManager;

    @Override
    public Response<ActivityCountDefinitionResponse> createActivityCountDefinition(CreateActivityCountDefinitionRequest request) {
        CreateActivityCountDefinitionCommand command = activityCountDefinitionApiAssembler.toCreateCommand(request, currentOperator());
        return Response.success(activityCountDefinitionApiAssembler.toResponse(
                activityCountDefinitionApplicationService.create(command)));
    }

    @Override
    public Response<PageResult<ActivityCountDefinitionResponse>> searchActivityCountDefinitions(ActivityCountDefinitionSearchRequest request) {
        ActivityCountDefinitionSearchQuery query = activityCountDefinitionApiAssembler.toSearchQuery(request);
        return Response.success(activityCountDefinitionApiAssembler.toPageResponse(
                activityCountDefinitionSearchService.search(query)));
    }

    @Override
    public Response<ActivityCountDefinitionResponse> getActivityCountDefinition(Long activityCountId) {
        return Response.success(activityCountDefinitionApiAssembler.toResponse(
                activityCountDefinitionApplicationService.queryByActivityCountId(activityCountId)));
    }

    @Override
    public Response<ActivityCountDefinitionResponse> updateActivityCountDefinition(Long activityCountId, UpdateActivityCountDefinitionRequest request) {
        UpdateActivityCountDefinitionCommand command = activityCountDefinitionApiAssembler.toUpdateCommand(
                activityCountId, request, currentOperator());
        return Response.success(activityCountDefinitionApiAssembler.toResponse(
                activityCountDefinitionApplicationService.update(command)));
    }

    private String currentOperator() {
        return adminSessionManager.currentOperator().username();
    }
}
