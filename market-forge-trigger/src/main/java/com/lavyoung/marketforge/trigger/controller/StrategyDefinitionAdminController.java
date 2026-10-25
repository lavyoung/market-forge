package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.admin.auth.response.AdminOperatorResponse;
import com.lavyoung.marketforge.api.strategy.IStrategyDefinitionAdminApi;
import com.lavyoung.marketforge.api.strategy.request.ChangeStrategyDefinitionStateRequest;
import com.lavyoung.marketforge.api.strategy.request.CreateStrategyDefinitionRequest;
import com.lavyoung.marketforge.api.strategy.request.UpdateStrategyDefinitionRequest;
import com.lavyoung.marketforge.api.strategy.response.StrategyDefinitionResponse;
import com.lavyoung.marketforge.application.strategy.model.*;
import com.lavyoung.marketforge.application.strategy.service.IStrategyDefinitionApplicationService;
import com.lavyoung.marketforge.trigger.assembler.StrategyDefinitionApiAssembler;
import com.lavyoung.marketforge.trigger.security.AdminSessionManager;
import com.lavyoung.marketforge.types.model.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * 策略定义运营管理 HTTP 适配器。
 *
 * <p>负责提取认证操作人、组合 HTTP 路径与请求体、调用策略定义应用服务，
 * 并将应用结果转换为稳定响应。该控制器不访问 Repository、DAO 或领域服务。</p>
 *
 * <p>详情查询不读取 Session 操作人；创建、修改、复制、发布和停用等写操作
 * 统一使用服务端认证身份，防止客户端伪造审计字段。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@RestController
@RequiredArgsConstructor
public class StrategyDefinitionAdminController implements IStrategyDefinitionAdminApi {

    private final IStrategyDefinitionApplicationService strategyDefinitionApplicationService;
    private final StrategyDefinitionApiAssembler strategyDefinitionApiAssembler;
    private final AdminSessionManager adminSessionManager;

    @Override
    public Response<StrategyDefinitionResponse> createStrategyDefinition(CreateStrategyDefinitionRequest request) {
        CreateStrategyDefinitionCommand command = strategyDefinitionApiAssembler.toCreateCommand(request, currentOperator());
        StrategyDefinitionResult result = strategyDefinitionApplicationService.create(command);
        return Response.success(strategyDefinitionApiAssembler.toResponse(result));
    }

    @Override
    public Response<StrategyDefinitionResponse> getStrategyDefinition(Long strategyId) {
        StrategyDefinitionResult result = strategyDefinitionApplicationService.queryByStrategyId(strategyId);
        return Response.success(strategyDefinitionApiAssembler.toResponse(result));
    }

    @Override
    public Response<StrategyDefinitionResponse> updateStrategyDefinition(Long strategyId, UpdateStrategyDefinitionRequest request) {
        UpdateStrategyDefinitionCommand command = strategyDefinitionApiAssembler.toUpdateCommand(strategyId, request, currentOperator());
        StrategyDefinitionResult result = strategyDefinitionApplicationService.updateDraft(command);
        return Response.success(strategyDefinitionApiAssembler.toResponse(result));
    }

    @Override
    public Response<StrategyDefinitionResponse> copyStrategyDefinition(Long strategyId) {
        CopyStrategyDefinitionCommand command = strategyDefinitionApiAssembler.toCopyCommand(strategyId, currentOperator());
        StrategyDefinitionResult result = strategyDefinitionApplicationService.copyAsDraft(command);
        return Response.success(strategyDefinitionApiAssembler.toResponse(result));
    }

    @Override
    public Response<StrategyDefinitionResponse> changeStrategyDefinitionState(Long strategyId,
                                                                              ChangeStrategyDefinitionStateRequest request) {
        ChangeStrategyDefinitionStateCommand command = strategyDefinitionApiAssembler.toStateCommand(
                strategyId, request, currentOperator());
        StrategyDefinitionResult result = strategyDefinitionApplicationService.changeState(command);
        return Response.success(strategyDefinitionApiAssembler.toResponse(result));
    }

    /**
     * 获取当前已经通过认证的运营人员标识。
     *
     * @return 当前登录运营人员用户名
     * @throws org.springframework.security.authentication.AuthenticationCredentialsNotFoundException 当前请求没有有效认证会话时抛出
     */
    private String currentOperator() {
        AdminOperatorResponse operator = adminSessionManager.currentOperator();
        return operator.username();
    }
}
