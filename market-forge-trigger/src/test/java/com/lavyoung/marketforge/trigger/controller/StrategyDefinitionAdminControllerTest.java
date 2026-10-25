package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.admin.auth.response.AdminOperatorResponse;
import com.lavyoung.marketforge.api.strategy.IStrategyAdminApi;
import com.lavyoung.marketforge.api.strategy.request.ChangeStrategyDefinitionStateRequest;
import com.lavyoung.marketforge.api.strategy.request.CreateStrategyDefinitionRequest;
import com.lavyoung.marketforge.api.strategy.request.UpdateStrategyDefinitionRequest;
import com.lavyoung.marketforge.api.strategy.response.StrategyDefinitionResponse;
import com.lavyoung.marketforge.application.strategy.model.*;
import com.lavyoung.marketforge.application.strategy.service.IStrategyDefinitionApplicationService;
import com.lavyoung.marketforge.application.strategy.service.IStrategySnapshotRefreshApplicationService;
import com.lavyoung.marketforge.trigger.assembler.StrategyDefinitionApiAssembler;
import com.lavyoung.marketforge.trigger.security.AdminSessionManager;
import com.lavyoung.marketforge.types.model.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * 策略定义运营 Controller 测试。
 *
 * <p>验证 Controller 只负责认证身份、协议转换、应用服务委托和响应包装，
 * 并确认定义接口与已有概率快照刷新接口可以共享策略资源根路径。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class StrategyDefinitionAdminControllerTest {

    private static final Long STRATEGY_ID = 100001L;
    private static final String OPERATOR = "admin";

    @Mock
    private IStrategyDefinitionApplicationService applicationService;
    @Mock
    private StrategyDefinitionApiAssembler assembler;
    @Mock
    private AdminSessionManager adminSessionManager;

    private StrategyDefinitionAdminController controller;

    @BeforeEach
    void setUp() {
        controller = new StrategyDefinitionAdminController(applicationService, assembler, adminSessionManager);
    }

    /**
     * Given 创建请求和登录运营人员，When 创建策略，Then 应使用认证身份生成命令并包装响应。
     */
    @Test
    void shouldCreateStrategyWithAuthenticatedOperator() {
        // Given
        CreateStrategyDefinitionRequest request = new CreateStrategyDefinitionRequest("测试策略", null, List.of(), List.of());
        CreateStrategyDefinitionCommand command = new CreateStrategyDefinitionCommand("测试策略", null, List.of(), List.of(), OPERATOR);
        StrategyDefinitionResult result = result();
        StrategyDefinitionResponse response = response();
        when(adminSessionManager.currentOperator()).thenReturn(operator());
        when(assembler.toCreateCommand(request, OPERATOR)).thenReturn(command);
        when(applicationService.create(command)).thenReturn(result);
        when(assembler.toResponse(result)).thenReturn(response);

        // When
        Response<StrategyDefinitionResponse> actual = controller.createStrategyDefinition(request);

        // Then
        assertSame(response, actual.data());
        verify(applicationService).create(command);
    }

    /**
     * Given 策略标识，When 查询详情，Then 不应读取只供写操作使用的 Session 操作人。
     */
    @Test
    void shouldQueryStrategyWithoutReadingOperatorSession() {
        // Given
        StrategyDefinitionResult result = result();
        StrategyDefinitionResponse response = response();
        when(applicationService.queryByStrategyId(STRATEGY_ID)).thenReturn(result);
        when(assembler.toResponse(result)).thenReturn(response);

        // When
        Response<StrategyDefinitionResponse> actual = controller.getStrategyDefinition(STRATEGY_ID);

        // Then
        assertSame(response, actual.data());
        verify(adminSessionManager, never()).currentOperator();
    }

    /**
     * Given 修改请求，When 更新草稿，Then 应组合路径标识和认证操作人。
     */
    @Test
    void shouldUpdateDraftWithPathAndAuthenticatedOperator() {
        // Given
        UpdateStrategyDefinitionRequest request = new UpdateStrategyDefinitionRequest("修改策略", null, List.of(), List.of(), 1);
        UpdateStrategyDefinitionCommand command = new UpdateStrategyDefinitionCommand(
                STRATEGY_ID, "修改策略", null, List.of(), List.of(), 1, OPERATOR);
        when(adminSessionManager.currentOperator()).thenReturn(operator());
        when(assembler.toUpdateCommand(STRATEGY_ID, request, OPERATOR)).thenReturn(command);
        when(applicationService.updateDraft(command)).thenReturn(result());
        when(assembler.toResponse(result())).thenReturn(response());

        // When
        controller.updateStrategyDefinition(STRATEGY_ID, request);

        // Then
        verify(applicationService).updateDraft(command);
    }

    /**
     * Given 历史策略标识，When 复制策略，Then 应只使用路径标识和认证身份创建命令。
     */
    @Test
    void shouldCopyStrategyWithAuthenticatedOperator() {
        // Given
        CopyStrategyDefinitionCommand command = new CopyStrategyDefinitionCommand(STRATEGY_ID, OPERATOR);
        when(adminSessionManager.currentOperator()).thenReturn(operator());
        when(assembler.toCopyCommand(STRATEGY_ID, OPERATOR)).thenReturn(command);
        when(applicationService.copyAsDraft(command)).thenReturn(result());
        when(assembler.toResponse(result())).thenReturn(response());

        // When
        controller.copyStrategyDefinition(STRATEGY_ID);

        // Then
        verify(applicationService).copyAsDraft(command);
    }

    /**
     * Given 状态请求，When 发布策略，Then 应把认证操作人传入状态应用命令。
     */
    @Test
    void shouldChangeStateWithAuthenticatedOperator() {
        // Given
        ChangeStrategyDefinitionStateRequest request = new ChangeStrategyDefinitionStateRequest("active", 1);
        ChangeStrategyDefinitionStateCommand command = new ChangeStrategyDefinitionStateCommand(
                STRATEGY_ID, "active", 1, OPERATOR);
        when(adminSessionManager.currentOperator()).thenReturn(operator());
        when(assembler.toStateCommand(STRATEGY_ID, request, OPERATOR)).thenReturn(command);
        when(applicationService.changeState(command)).thenReturn(result());
        when(assembler.toResponse(result())).thenReturn(response());

        // When
        controller.changeStrategyDefinitionState(STRATEGY_ID, request);

        // Then
        verify(applicationService).changeState(command);
    }

    /**
     * Given 定义 Controller 与快照刷新 Controller 同时注册，When 请求刷新路径，Then 路由不应冲突。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldCoexistWithSnapshotRefreshRoute() throws Exception {
        // Given
        IStrategySnapshotRefreshApplicationService refreshService = org.mockito.Mockito.mock(
                IStrategySnapshotRefreshApplicationService.class);
        when(refreshService.requestRefresh(STRATEGY_ID)).thenReturn(true);
        MockMvc mockMvc = standaloneSetup(controller, new StrategyAdminController(refreshService)).build();

        // When & Then
        mockMvc.perform(post(IStrategyAdminApi.BASE_PATH + "/{strategyId}/snapshot-refresh", STRATEGY_ID))
                .andExpect(status().isAccepted());
        verify(refreshService).requestRefresh(STRATEGY_ID);
    }

    private AdminOperatorResponse operator() {
        return new AdminOperatorResponse(OPERATOR, "系统管理员", Set.of("ROLE_ADMIN"));
    }

    private StrategyDefinitionResult result() {
        return new StrategyDefinitionResult(STRATEGY_ID, "测试策略", null, "draft", 1,
                OPERATOR, OPERATOR, null, null, List.of(), List.of());
    }

    private StrategyDefinitionResponse response() {
        return new StrategyDefinitionResponse(STRATEGY_ID.toString(), "测试策略", null, "draft", 1,
                OPERATOR, OPERATOR, null, null, List.of(), List.of());
    }
}
