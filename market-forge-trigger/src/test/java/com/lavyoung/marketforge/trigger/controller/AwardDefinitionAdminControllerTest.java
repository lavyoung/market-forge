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
import com.lavyoung.marketforge.trigger.exception.GlobalExceptionHandler;
import com.lavyoung.marketforge.trigger.security.AdminSessionManager;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import com.lavyoung.marketforge.types.model.PageRequest;
import com.lavyoung.marketforge.types.model.PageResult;
import com.lavyoung.marketforge.types.model.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * {@link AwardDefinitionAdminController} 单元测试。
 *
 * <p>验证 Controller 只负责认证身份提取、协议装配、应用服务委托和统一响应包装，
 * 不在 HTTP 适配层重新实现奖品领域规则。</p>
 */
@ExtendWith(MockitoExtension.class)
class AwardDefinitionAdminControllerTest {

    private static final Long AWARD_ID = 9_007_199_254_740_993L;
    private static final String OPERATOR = "admin";

    @Mock
    private IAwardDefinitionApplicationService awardDefinitionApplicationService;

    @Mock
    private IAwardDefinitionSearchService awardDefinitionSearchService;

    @Mock
    private AwardDefinitionApiAssembler awardDefinitionApiAssembler;

    @Mock
    private AdminSessionManager adminSessionManager;

    private AwardDefinitionAdminController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new AwardDefinitionAdminController(awardDefinitionApplicationService, awardDefinitionSearchService, awardDefinitionApiAssembler, adminSessionManager);
        mockMvc = standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    /**
     * 验证 GET 查询参数能够绑定为查询请求，并返回统一分页 JSON 结构。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldExposeAwardDefinitionSearchHttpContract() throws Exception {
        // Given
        AwardDefinitionSearchRequest request = new AwardDefinitionSearchRequest("credit_100", "积分", "internal_credit", "active", 1, 20);
        AwardDefinitionSearchQuery query = new AwardDefinitionSearchQuery("credit_100", "积分", "internal_credit", "active", new PageRequest(1, 20));
        PageResult<AwardDefinitionResult> applicationPage = new PageResult<>(List.of(result("active", 2)), 1, 1, 20);
        PageResult<AwardDefinitionResponse> responsePage = new PageResult<>(List.of(response("active", 2)), 1, 1, 20);
        when(awardDefinitionApiAssembler.toSearchQuery(request)).thenReturn(query);
        when(awardDefinitionSearchService.search(query)).thenReturn(applicationPage);
        when(awardDefinitionApiAssembler.toPageResponse(applicationPage)).thenReturn(responsePage);

        // When & Then
        mockMvc.perform(get(IAwardDefinitionAdminApi.BASE_PATH)
                        .param("awardCode", "credit_100")
                        .param("awardName", "积分")
                        .param("awardType", "internal_credit")
                        .param("state", "active")
                        .param("pageNumber", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.pageNumber").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.records[0].awardId").value(AWARD_ID.toString()))
                .andExpect(jsonPath("$.data.records[0].awardCode").value("credit_100"));

        verify(awardDefinitionSearchService).search(query);
    }

    /**
     * 验证非法分页参数在 HTTP 协议边界被拒绝，不进入应用查询服务。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldRejectInvalidSearchPaginationBeforeApplicationService() throws Exception {
        // When & Then
        mockMvc.perform(get(IAwardDefinitionAdminApi.BASE_PATH).param("pageNumber", "0").param("pageSize", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.PARAM_INVALID.getCode()));

        verifyNoInteractions(awardDefinitionSearchService);
    }

    /**
     * 验证分页查询只完成协议转换和只读应用服务委托，不读取写操作所需的操作人。
     */
    @Test
    void shouldSearchAwardDefinitionsWithoutReadingOperatorSession() {
        // Given
        AwardDefinitionSearchRequest request = new AwardDefinitionSearchRequest(null, null, "internal_credit", "active", 1, 20);
        AwardDefinitionSearchQuery query = new AwardDefinitionSearchQuery(null, null, "internal_credit", "active", new PageRequest(1, 20));
        PageResult<AwardDefinitionResult> applicationPage = new PageResult<>(List.of(result("active", 2)), 1, 1, 20);
        PageResult<AwardDefinitionResponse> responsePage = new PageResult<>(List.of(response("active", 2)), 1, 1, 20);
        when(awardDefinitionApiAssembler.toSearchQuery(request)).thenReturn(query);
        when(awardDefinitionSearchService.search(query)).thenReturn(applicationPage);
        when(awardDefinitionApiAssembler.toPageResponse(applicationPage)).thenReturn(responsePage);

        // When
        Response<PageResult<AwardDefinitionResponse>> actual = controller.searchAwardDefinitions(request);

        // Then
        assertSame(responsePage, actual.data());
        verify(awardDefinitionSearchService).search(query);
        verify(adminSessionManager, never()).currentOperator();
    }

    /**
     * 验证创建操作使用服务端认证用户名，而不是接收客户端操作人。
     */
    @Test
    void shouldCreateAwardWithAuthenticatedOperator() {
        // Given
        CreateAwardDefinitionRequest request = createRequest();
        CreateAwardDefinitionCommand command = new CreateAwardDefinitionCommand(
                "credit_100", "100积分", "internal_credit", "automatic", null,
                "{\"amount\":100}", 1, "中奖后自动发放积分", OPERATOR);
        AwardDefinitionResult result = result("draft", 1);
        AwardDefinitionResponse response = response("draft", 1);
        when(adminSessionManager.currentOperator()).thenReturn(operator());
        when(awardDefinitionApiAssembler.toCreateCommand(request, OPERATOR)).thenReturn(command);
        when(awardDefinitionApplicationService.create(command)).thenReturn(result);
        when(awardDefinitionApiAssembler.toResponse(result)).thenReturn(response);

        // When
        Response<AwardDefinitionResponse> actual = controller.createAwardDefinition(request);

        // Then
        assertSame(response, actual.data());
        verify(adminSessionManager).currentOperator();
        verify(awardDefinitionApplicationService).create(command);
    }

    /**
     * 验证详情接口只委托按奖品编号查询，不重复读取操作人。
     */
    @Test
    void shouldQueryAwardDefinitionWithoutDuplicatingAuthenticationMapping() {
        // Given
        AwardDefinitionResult result = result("active", 2);
        AwardDefinitionResponse response = response("active", 2);
        when(awardDefinitionApplicationService.queryByAwardId(AWARD_ID)).thenReturn(result);
        when(awardDefinitionApiAssembler.toResponse(result)).thenReturn(response);

        // When
        Response<AwardDefinitionResponse> actual = controller.getAwardDefinition(AWARD_ID);

        // Then
        assertSame(response, actual.data());
        verify(awardDefinitionApplicationService).queryByAwardId(AWARD_ID);
        verify(adminSessionManager, never()).currentOperator();
    }

    /**
     * 验证修改操作组合路径奖品编号、请求内容和认证操作人。
     */
    @Test
    void shouldUpdateDraftWithPathAwardIdAndAuthenticatedOperator() {
        // Given
        UpdateAwardDefinitionRequest request = updateRequest();
        UpdateAwardDefinitionCommand command = new UpdateAwardDefinitionCommand(
                AWARD_ID, "credit_200", "200积分", "internal_credit", "automatic", null,
                "{\"amount\":200}", 2, "中奖后自动发放二百积分", 1, OPERATOR);
        AwardDefinitionResult result = result("draft", 2);
        AwardDefinitionResponse response = response("draft", 2);
        when(adminSessionManager.currentOperator()).thenReturn(operator());
        when(awardDefinitionApiAssembler.toUpdateCommand(AWARD_ID, request, OPERATOR)).thenReturn(command);
        when(awardDefinitionApplicationService.updateDraft(command)).thenReturn(result);
        when(awardDefinitionApiAssembler.toResponse(result)).thenReturn(response);

        // When
        Response<AwardDefinitionResponse> actual = controller.updateAwardDefinition(AWARD_ID, request);

        // Then
        assertSame(response, actual.data());
        verify(awardDefinitionApplicationService).updateDraft(command);
    }

    /**
     * 验证状态变更操作把认证操作人传递到应用命令。
     */
    @Test
    void shouldChangeStateWithAuthenticatedOperator() {
        // Given
        ChangeAwardDefinitionStateRequest request = new ChangeAwardDefinitionStateRequest("active", 1);
        ChangeAwardDefinitionStateCommand command = new ChangeAwardDefinitionStateCommand(AWARD_ID, "active", 1, OPERATOR);
        AwardDefinitionResult result = result("active", 2);
        AwardDefinitionResponse response = response("active", 2);
        when(adminSessionManager.currentOperator()).thenReturn(operator());
        when(awardDefinitionApiAssembler.toStateCommand(AWARD_ID, request, OPERATOR)).thenReturn(command);
        when(awardDefinitionApplicationService.changeState(command)).thenReturn(result);
        when(awardDefinitionApiAssembler.toResponse(result)).thenReturn(response);

        // When
        Response<AwardDefinitionResponse> actual = controller.changeAwardDefinitionState(AWARD_ID, request);

        // Then
        assertSame(response, actual.data());
        verify(awardDefinitionApplicationService).changeState(command);
    }

    /**
     * 验证未登录请求在读取身份时立即失败，不产生任何奖品写入调用。
     */
    @Test
    void shouldRejectCreateWhenAdminSessionIsMissing() {
        // Given
        CreateAwardDefinitionRequest request = createRequest();
        AuthenticationCredentialsNotFoundException expected =
                new AuthenticationCredentialsNotFoundException("Admin authentication is required");
        when(adminSessionManager.currentOperator()).thenThrow(expected);

        // When
        AuthenticationCredentialsNotFoundException actual = assertThrows(
                AuthenticationCredentialsNotFoundException.class,
                () -> controller.createAwardDefinition(request));

        // Then
        assertEquals(expected.getMessage(), actual.getMessage());
        verify(awardDefinitionApiAssembler, never()).toCreateCommand(request, OPERATOR);
        verify(awardDefinitionApplicationService, never()).create(org.mockito.ArgumentMatchers.any());
    }

    private CreateAwardDefinitionRequest createRequest() {
        return new CreateAwardDefinitionRequest(
                "credit_100", "100积分", "internal_credit", "automatic", null,
                "{\"amount\":100}", 1, "中奖后自动发放积分");
    }

    private UpdateAwardDefinitionRequest updateRequest() {
        return new UpdateAwardDefinitionRequest(
                "credit_200", "200积分", "internal_credit", "automatic", null,
                "{\"amount\":200}", 2, "中奖后自动发放二百积分", 1);
    }

    private AdminOperatorResponse operator() {
        return new AdminOperatorResponse(OPERATOR, "系统管理员", Set.of("ROLE_ADMIN"));
    }

    private AwardDefinitionResult result(String state, int version) {
        return new AwardDefinitionResult(
                AWARD_ID, "credit_100", "100积分", "internal_credit", "automatic", null,
                "{\"amount\":100}", 1, "中奖后自动发放积分", state, version,
                OPERATOR, OPERATOR, LocalDateTime.of(2026, 10, 22, 10, 0),
                LocalDateTime.of(2026, 10, 22, 10, 30));
    }

    private AwardDefinitionResponse response(String state, int version) {
        return new AwardDefinitionResponse(
                AWARD_ID.toString(), "credit_100", "100积分", "internal_credit", "automatic", null,
                "{\"amount\":100}", 1, "中奖后自动发放积分", state, version,
                OPERATOR, OPERATOR, LocalDateTime.of(2026, 10, 22, 10, 0),
                LocalDateTime.of(2026, 10, 22, 10, 30));
    }
}
