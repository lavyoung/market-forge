package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.activity.IActivitySkuDefinitionAdminApi;
import com.lavyoung.marketforge.api.activity.request.ActivitySkuDefinitionSearchRequest;
import com.lavyoung.marketforge.api.activity.request.AdjustActivitySkuStockRequest;
import com.lavyoung.marketforge.api.activity.request.CreateActivitySkuDefinitionRequest;
import com.lavyoung.marketforge.api.activity.request.UpdateActivitySkuDefinitionRequest;
import com.lavyoung.marketforge.api.activity.response.ActivitySkuDefinitionResponse;
import com.lavyoung.marketforge.application.activity.model.ActivitySkuDefinitionSearchQuery;
import com.lavyoung.marketforge.application.activity.model.AdjustActivitySkuStockCommand;
import com.lavyoung.marketforge.application.activity.model.CreateActivitySkuDefinitionCommand;
import com.lavyoung.marketforge.application.activity.model.UpdateActivitySkuDefinitionCommand;
import com.lavyoung.marketforge.application.activity.service.IActivitySkuDefinitionApplicationService;
import com.lavyoung.marketforge.application.activity.service.IActivitySkuDefinitionSearchService;
import com.lavyoung.marketforge.trigger.assembler.ActivitySkuDefinitionApiAssembler;
import com.lavyoung.marketforge.trigger.security.AdminSessionManager;
import com.lavyoung.marketforge.types.model.PageResult;
import com.lavyoung.marketforge.types.model.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * 活动 SKU 运营管理 HTTP 适配器。
 *
 * <p>Controller 只负责协议转换、认证身份提取和应用服务调用，不承载活动生命周期、
 * 次数配置关系、库存计算或乐观锁规则。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
@RestController
@RequiredArgsConstructor
public class ActivitySkuDefinitionAdminController implements IActivitySkuDefinitionAdminApi {

    private final IActivitySkuDefinitionApplicationService activitySkuDefinitionApplicationService;
    private final IActivitySkuDefinitionSearchService activitySkuDefinitionSearchService;
    private final ActivitySkuDefinitionApiAssembler activitySkuDefinitionApiAssembler;
    private final AdminSessionManager adminSessionManager;

    @Override
    public Response<ActivitySkuDefinitionResponse> createActivitySkuDefinition(CreateActivitySkuDefinitionRequest request) {
        CreateActivitySkuDefinitionCommand command = activitySkuDefinitionApiAssembler.toCreateCommand(request, currentOperator());
        return Response.success(activitySkuDefinitionApiAssembler.toResponse(
                activitySkuDefinitionApplicationService.create(command)));
    }

    @Override
    public Response<PageResult<ActivitySkuDefinitionResponse>> searchActivitySkuDefinitions(ActivitySkuDefinitionSearchRequest request) {
        ActivitySkuDefinitionSearchQuery query = activitySkuDefinitionApiAssembler.toSearchQuery(request);
        return Response.success(activitySkuDefinitionApiAssembler.toPageResponse(
                activitySkuDefinitionSearchService.search(query)));
    }

    @Override
    public Response<ActivitySkuDefinitionResponse> getActivitySkuDefinition(Long sku) {
        return Response.success(activitySkuDefinitionApiAssembler.toResponse(
                activitySkuDefinitionApplicationService.queryBySku(sku)));
    }

    @Override
    public Response<ActivitySkuDefinitionResponse> updateActivitySkuDefinition(Long sku, UpdateActivitySkuDefinitionRequest request) {
        UpdateActivitySkuDefinitionCommand command = activitySkuDefinitionApiAssembler.toUpdateCommand(
                sku, request, currentOperator());
        return Response.success(activitySkuDefinitionApiAssembler.toResponse(
                activitySkuDefinitionApplicationService.updateConfiguration(command)));
    }

    @Override
    public Response<ActivitySkuDefinitionResponse> adjustActivitySkuStock(Long sku, AdjustActivitySkuStockRequest request) {
        AdjustActivitySkuStockCommand command = activitySkuDefinitionApiAssembler.toAdjustStockCommand(
                sku, request, currentOperator());
        return Response.success(activitySkuDefinitionApiAssembler.toResponse(
                activitySkuDefinitionApplicationService.adjustStock(command)));
    }

    private String currentOperator() {
        return adminSessionManager.currentOperator().username();
    }
}
