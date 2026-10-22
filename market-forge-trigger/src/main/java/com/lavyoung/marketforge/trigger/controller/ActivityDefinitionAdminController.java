package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.activity.IActivityDefinitionAdminApi;
import com.lavyoung.marketforge.api.activity.request.ActivityDefinitionSearchRequest;
import com.lavyoung.marketforge.api.activity.request.ChangeActivityDefinitionStateRequest;
import com.lavyoung.marketforge.api.activity.request.CreateActivityDefinitionRequest;
import com.lavyoung.marketforge.api.activity.request.UpdateActivityDefinitionRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityDefinitionResponse;
import com.lavyoung.marketforge.api.admin.auth.response.AdminOperatorResponse;
import com.lavyoung.marketforge.application.activity.model.*;
import com.lavyoung.marketforge.application.activity.service.IActivityDefinitionApplicationService;
import com.lavyoung.marketforge.application.activity.service.IActivityDefinitionSearchService;
import com.lavyoung.marketforge.trigger.assembler.ActivityDefinitionApiAssembler;
import com.lavyoung.marketforge.trigger.security.AdminSessionManager;
import com.lavyoung.marketforge.types.model.PageResult;
import com.lavyoung.marketforge.types.model.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * 活动定义运营 HTTP 适配器。
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
    private final IActivityDefinitionApplicationService activityDefinitionApplicationService;
    private final ActivityDefinitionApiAssembler activityDefinitionApiAssembler;
    private final AdminSessionManager adminSessionManager;

    @Override
    public Response<ActivityDefinitionResponse> createActivityDefinition(CreateActivityDefinitionRequest request) {
        CreateActivityDefinitionCommand command = activityDefinitionApiAssembler.toCreateCommand(request, currentOperator());
        return Response.success(activityDefinitionApiAssembler.toResponse(activityDefinitionApplicationService.create(command)));
    }

    @Override
    public Response<PageResult<ActivityDefinitionResponse>> searchActivityDefinitions(ActivityDefinitionSearchRequest request) {
        ActivityDefinitionSearchQuery query = activityDefinitionApiAssembler.toSearchQuery(request);
        PageResult<ActivityDefinitionResult> result = activityDefinitionSearchService.search(query);
        return Response.success(activityDefinitionApiAssembler.toPageResponse(result));
    }

    @Override
    public Response<ActivityDefinitionResponse> getActivityDefinition(Long activityId) {
        return Response.success(activityDefinitionApiAssembler.toResponse(
                activityDefinitionApplicationService.queryByActivityId(activityId)));
    }

    @Override
    public Response<ActivityDefinitionResponse> updateActivityDefinition(Long activityId,
                                                                         UpdateActivityDefinitionRequest request) {
        UpdateActivityDefinitionCommand command = activityDefinitionApiAssembler.toUpdateCommand(activityId, request,
                currentOperator());
        return Response.success(activityDefinitionApiAssembler.toResponse(
                activityDefinitionApplicationService.updateDraft(command)));
    }

    @Override
    public Response<ActivityDefinitionResponse> changeActivityDefinitionState(Long activityId,
                                                                              ChangeActivityDefinitionStateRequest request) {
        ChangeActivityDefinitionStateCommand command = activityDefinitionApiAssembler.toStateCommand(activityId,
                request, currentOperator());
        return Response.success(activityDefinitionApiAssembler.toResponse(
                activityDefinitionApplicationService.changeState(command)));
    }

    private String currentOperator() {
        AdminOperatorResponse operator = adminSessionManager.currentOperator();
        return operator.username();
    }
}
