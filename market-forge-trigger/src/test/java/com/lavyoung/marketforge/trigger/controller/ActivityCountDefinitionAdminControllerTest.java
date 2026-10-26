package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.activity.request.ActivityCountDefinitionSearchRequest;
import com.lavyoung.marketforge.api.activity.request.CreateActivityCountDefinitionRequest;
import com.lavyoung.marketforge.api.activity.request.UpdateActivityCountDefinitionRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityCountDefinitionResponse;
import com.lavyoung.marketforge.api.admin.auth.response.AdminOperatorResponse;
import com.lavyoung.marketforge.application.activity.model.ActivityCountDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivityCountDefinitionSearchQuery;
import com.lavyoung.marketforge.application.activity.model.CreateActivityCountDefinitionCommand;
import com.lavyoung.marketforge.application.activity.model.UpdateActivityCountDefinitionCommand;
import com.lavyoung.marketforge.application.activity.service.IActivityCountDefinitionApplicationService;
import com.lavyoung.marketforge.application.activity.service.IActivityCountDefinitionSearchService;
import com.lavyoung.marketforge.trigger.assembler.ActivityCountDefinitionApiAssembler;
import com.lavyoung.marketforge.trigger.security.AdminSessionManager;
import com.lavyoung.marketforge.types.model.PageRequest;
import com.lavyoung.marketforge.types.model.PageResult;
import com.lavyoung.marketforge.types.model.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * 活动次数配置运营 Controller 测试。
 *
 * <p>验证写请求使用服务端认证身份，查询请求不读取操作人会话。</p>
 */
@ExtendWith(MockitoExtension.class)
class ActivityCountDefinitionAdminControllerTest {

    @Mock
    private IActivityCountDefinitionApplicationService applicationService;

    @Mock
    private IActivityCountDefinitionSearchService searchService;

    @Mock
    private ActivityCountDefinitionApiAssembler assembler;

    @Mock
    private AdminSessionManager adminSessionManager;

    @InjectMocks
    private ActivityCountDefinitionAdminController controller;

    @Test
    void shouldCreateDefinitionWithAuthenticatedOperator() {
        // Given
        CreateActivityCountDefinitionRequest request = new CreateActivityCountDefinitionRequest(
                "签到次数包", 100, 10, 50);
        CreateActivityCountDefinitionCommand command = new CreateActivityCountDefinitionCommand(
                "签到次数包", 100, 10, 50, "admin");
        ActivityCountDefinitionResult result = mock(ActivityCountDefinitionResult.class);
        ActivityCountDefinitionResponse expected = mock(ActivityCountDefinitionResponse.class);
        when(adminSessionManager.currentOperator()).thenReturn(
                new AdminOperatorResponse("admin", "系统管理员", Set.of("ROLE_ADMIN")));
        when(assembler.toCreateCommand(request, "admin")).thenReturn(command);
        when(applicationService.create(command)).thenReturn(result);
        when(assembler.toResponse(result)).thenReturn(expected);

        // When
        Response<ActivityCountDefinitionResponse> actual = controller.createActivityCountDefinition(request);

        // Then
        assertEquals(expected, actual.data());
        verify(assembler).toCreateCommand(request, "admin");
    }

    @Test
    void shouldUpdateDefinitionWithPathIdAndAuthenticatedOperator() {
        // Given
        UpdateActivityCountDefinitionRequest request = new UpdateActivityCountDefinitionRequest(
                "积分兑换次数包", 200, 20, 100, 2);
        UpdateActivityCountDefinitionCommand command = new UpdateActivityCountDefinitionCommand(
                100301L, "积分兑换次数包", 200, 20, 100, 2, "admin");
        ActivityCountDefinitionResult result = mock(ActivityCountDefinitionResult.class);
        ActivityCountDefinitionResponse expected = mock(ActivityCountDefinitionResponse.class);
        when(adminSessionManager.currentOperator()).thenReturn(
                new AdminOperatorResponse("admin", "系统管理员", Set.of("ROLE_ADMIN")));
        when(assembler.toUpdateCommand(100301L, request, "admin")).thenReturn(command);
        when(applicationService.update(command)).thenReturn(result);
        when(assembler.toResponse(result)).thenReturn(expected);

        // When
        Response<ActivityCountDefinitionResponse> actual = controller.updateActivityCountDefinition(100301L, request);

        // Then
        assertEquals(expected, actual.data());
        verify(applicationService).update(command);
    }

    @Test
    void shouldDelegateSearchWithoutReadingOperatorSession() {
        // Given
        ActivityCountDefinitionSearchRequest request = new ActivityCountDefinitionSearchRequest(
                null, "签到", 1, 20);
        ActivityCountDefinitionSearchQuery query = new ActivityCountDefinitionSearchQuery(
                null, "签到", new PageRequest(1, 20));
        PageResult<ActivityCountDefinitionResult> result = PageResult.empty(1, 20);
        PageResult<ActivityCountDefinitionResponse> expected = PageResult.empty(1, 20);
        when(assembler.toSearchQuery(request)).thenReturn(query);
        when(searchService.search(query)).thenReturn(result);
        when(assembler.toPageResponse(result)).thenReturn(expected);

        // When
        Response<PageResult<ActivityCountDefinitionResponse>> actual = controller.searchActivityCountDefinitions(request);

        // Then
        assertEquals(expected, actual.data());
        verifyNoInteractions(adminSessionManager);
    }
}
