package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.activity.IActivityStageAdminApi;
import com.lavyoung.marketforge.api.activity.request.ActivityStageSearchRequest;
import com.lavyoung.marketforge.api.activity.request.CreateActivityStageRequest;
import com.lavyoung.marketforge.api.activity.request.PublishActivityStageRequest;
import com.lavyoung.marketforge.api.activity.request.UpdateActivityStageRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityStageResponse;
import com.lavyoung.marketforge.application.activity.model.ActivityStageSearchQuery;
import com.lavyoung.marketforge.application.activity.model.CreateActivityStageCommand;
import com.lavyoung.marketforge.application.activity.model.PublishActivityStageCommand;
import com.lavyoung.marketforge.application.activity.model.UpdateActivityStageCommand;
import com.lavyoung.marketforge.application.activity.service.IActivityStageApplicationService;
import com.lavyoung.marketforge.application.activity.service.IActivityStageSearchService;
import com.lavyoung.marketforge.trigger.assembler.ActivityStageApiAssembler;
import com.lavyoung.marketforge.trigger.security.AdminSessionManager;
import com.lavyoung.marketforge.types.model.PageResult;
import com.lavyoung.marketforge.types.model.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * 活动展台运营管理 HTTP 适配器。
 *
 * <p>Controller 只完成协议转换、认证身份提取和应用服务调用。活动可否发布、预热顺序、
 * 原生状态替换与乐观锁冲突均由 Application 和 Domain 层处理。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@RestController
@RequiredArgsConstructor
public class ActivityStageAdminController implements IActivityStageAdminApi {

    private final IActivityStageApplicationService activityStageApplicationService;
    private final IActivityStageSearchService activityStageSearchService;
    private final ActivityStageApiAssembler activityStageApiAssembler;
    private final AdminSessionManager adminSessionManager;

    @Override
    public Response<ActivityStageResponse> createActivityStage(CreateActivityStageRequest request) {
        CreateActivityStageCommand command = activityStageApiAssembler.toCreateCommand(request, currentOperator());
        return Response.success(activityStageApiAssembler.toResponse(activityStageApplicationService.create(command)));
    }

    @Override
    public Response<PageResult<ActivityStageResponse>> searchActivityStages(ActivityStageSearchRequest request) {
        ActivityStageSearchQuery query = activityStageApiAssembler.toSearchQuery(request);
        return Response.success(activityStageApiAssembler.toPageResponse(activityStageSearchService.search(query)));
    }

    @Override
    public Response<ActivityStageResponse> getActivityStage(Long stageId) {
        return Response.success(activityStageApiAssembler.toResponse(activityStageApplicationService.queryByStageId(stageId)));
    }

    @Override
    public Response<ActivityStageResponse> updateActivityStage(Long stageId, UpdateActivityStageRequest request) {
        UpdateActivityStageCommand command = activityStageApiAssembler.toUpdateCommand(stageId, request, currentOperator());
        return Response.success(activityStageApiAssembler.toResponse(activityStageApplicationService.updateDraft(command)));
    }

    @Override
    public Response<ActivityStageResponse> publishActivityStage(Long stageId, PublishActivityStageRequest request) {
        PublishActivityStageCommand command = activityStageApiAssembler.toPublishCommand(stageId, request, currentOperator());
        return Response.success(activityStageApiAssembler.toResponse(activityStageApplicationService.publish(command)));
    }

    private String currentOperator() {
        return adminSessionManager.currentOperator().username();
    }
}
