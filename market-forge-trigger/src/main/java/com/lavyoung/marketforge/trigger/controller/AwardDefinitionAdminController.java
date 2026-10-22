package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.admin.auth.response.AdminOperatorResponse;
import com.lavyoung.marketforge.api.award.IAwardDefinitionAdminApi;
import com.lavyoung.marketforge.api.award.request.AwardDefinitionSearchRequest;
import com.lavyoung.marketforge.api.award.request.ChangeAwardDefinitionStateRequest;
import com.lavyoung.marketforge.api.award.request.CreateAwardDefinitionRequest;
import com.lavyoung.marketforge.api.award.request.UpdateAwardDefinitionRequest;
import com.lavyoung.marketforge.api.award.response.AwardDefinitionResponse;
import com.lavyoung.marketforge.application.award.model.*;
import com.lavyoung.marketforge.application.award.service.IAwardDefinitionApplicationService;
import com.lavyoung.marketforge.application.award.service.IAwardDefinitionSearchService;
import com.lavyoung.marketforge.trigger.assembler.AwardDefinitionApiAssembler;
import com.lavyoung.marketforge.trigger.security.AdminSessionManager;
import com.lavyoung.marketforge.types.model.PageResult;
import com.lavyoung.marketforge.types.model.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * 奖品定义运营管理 HTTP 适配器。
 *
 * <p>负责将运营后台 HTTP 请求转换为 Application Command，并把应用结果
 * 转换为稳定的 API 响应。该控制器不访问 DAO、Repository 或领域服务。</p>
 *
 * <p>写操作所需的操作人统一从已经认证的后台会话读取，不能信任客户端
 * 提交的用户名，从而保证奖品配置审计信息可信。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@RestController
@RequiredArgsConstructor
public class AwardDefinitionAdminController implements IAwardDefinitionAdminApi {

    private final IAwardDefinitionApplicationService awardDefinitionApplicationService;
    private final IAwardDefinitionSearchService awardDefinitionSearchService;
    private final AwardDefinitionApiAssembler awardDefinitionApiAssembler;
    private final AdminSessionManager adminSessionManager;

    @Override
    public Response<AwardDefinitionResponse> createAwardDefinition(CreateAwardDefinitionRequest request) {
        String operator = currentOperator();
        CreateAwardDefinitionCommand command = awardDefinitionApiAssembler.toCreateCommand(request, operator);
        AwardDefinitionResult result = awardDefinitionApplicationService.create(command);
        return Response.success(awardDefinitionApiAssembler.toResponse(result));
    }

    @Override
    public Response<PageResult<AwardDefinitionResponse>> searchAwardDefinitions(AwardDefinitionSearchRequest request) {
        AwardDefinitionSearchQuery query = awardDefinitionApiAssembler.toSearchQuery(request);
        PageResult<AwardDefinitionResult> result = awardDefinitionSearchService.search(query);
        return Response.success(awardDefinitionApiAssembler.toPageResponse(result));
    }

    @Override
    public Response<AwardDefinitionResponse> getAwardDefinition(Long awardId) {
        AwardDefinitionResult result = awardDefinitionApplicationService.queryByAwardId(awardId);
        return Response.success(awardDefinitionApiAssembler.toResponse(result));
    }

    @Override
    public Response<AwardDefinitionResponse> updateAwardDefinition(Long awardId, UpdateAwardDefinitionRequest request) {
        String operator = currentOperator();
        UpdateAwardDefinitionCommand command = awardDefinitionApiAssembler.toUpdateCommand(awardId, request, operator);
        AwardDefinitionResult result = awardDefinitionApplicationService.updateDraft(command);
        return Response.success(awardDefinitionApiAssembler.toResponse(result));
    }

    @Override
    public Response<AwardDefinitionResponse> changeAwardDefinitionState(Long awardId, ChangeAwardDefinitionStateRequest request) {
        String operator = currentOperator();
        ChangeAwardDefinitionStateCommand command = awardDefinitionApiAssembler.toStateCommand(awardId, request, operator);
        AwardDefinitionResult result = awardDefinitionApplicationService.changeState(command);
        return Response.success(awardDefinitionApiAssembler.toResponse(result));
    }

    /**
     * 获取当前已经通过认证的运营人员标识。
     *
     * <p>统一从服务端 SecurityContext 和 Session 获取，避免客户端伪造
     * 创建人或修改人。未登录时由会话管理器抛出认证异常。</p>
     *
     * @return 当前登录运营人员的用户名
     * @throws org.springframework.security.authentication.AuthenticationCredentialsNotFoundException 当前请求没有有效认证会话时抛出
     */
    private String currentOperator() {
        AdminOperatorResponse operator = adminSessionManager.currentOperator();
        return operator.username();
    }
}
