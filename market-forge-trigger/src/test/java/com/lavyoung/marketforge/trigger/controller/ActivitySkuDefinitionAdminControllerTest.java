package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.activity.request.ActivitySkuDefinitionSearchRequest;
import com.lavyoung.marketforge.api.activity.request.AdjustActivitySkuStockRequest;
import com.lavyoung.marketforge.api.activity.request.CreateActivitySkuDefinitionRequest;
import com.lavyoung.marketforge.api.activity.request.UpdateActivitySkuDefinitionRequest;
import com.lavyoung.marketforge.api.activity.response.ActivitySkuDefinitionResponse;
import com.lavyoung.marketforge.api.admin.auth.response.AdminOperatorResponse;
import com.lavyoung.marketforge.application.activity.model.*;
import com.lavyoung.marketforge.application.activity.service.IActivitySkuDefinitionApplicationService;
import com.lavyoung.marketforge.application.activity.service.IActivitySkuDefinitionSearchService;
import com.lavyoung.marketforge.trigger.assembler.ActivitySkuDefinitionApiAssembler;
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
 * 活动 SKU 运营 Controller 测试。
 *
 * <p>验证写操作使用服务端认证身份，库存调整不会信任客户端操作人，查询请求不会读取
 * 当前登录会话。</p>
 */
@ExtendWith(MockitoExtension.class)
class ActivitySkuDefinitionAdminControllerTest {

    private static final Long SKU = 900001L;

    @Mock
    private IActivitySkuDefinitionApplicationService applicationService;

    @Mock
    private IActivitySkuDefinitionSearchService searchService;

    @Mock
    private ActivitySkuDefinitionApiAssembler assembler;

    @Mock
    private AdminSessionManager adminSessionManager;

    @InjectMocks
    private ActivitySkuDefinitionAdminController controller;

    @Test
    void shouldCreateSkuWithAuthenticatedOperator() {
        // Given
        CreateActivitySkuDefinitionRequest request = new CreateActivitySkuDefinitionRequest(
                "100001", "300001", 100L, 500L);
        CreateActivitySkuDefinitionCommand command = new CreateActivitySkuDefinitionCommand(
                100001L, 300001L, 100L, 500L, "admin");
        ActivitySkuDefinitionResult result = mock(ActivitySkuDefinitionResult.class);
        ActivitySkuDefinitionResponse expected = mock(ActivitySkuDefinitionResponse.class);
        stubOperator();
        when(assembler.toCreateCommand(request, "admin")).thenReturn(command);
        when(applicationService.create(command)).thenReturn(result);
        when(assembler.toResponse(result)).thenReturn(expected);

        // When
        Response<ActivitySkuDefinitionResponse> actual = controller.createActivitySkuDefinition(request);

        // Then
        assertEquals(expected, actual.data());
        verify(assembler).toCreateCommand(request, "admin");
    }

    @Test
    void shouldDelegateSearchWithoutReadingOperatorSession() {
        // Given
        ActivitySkuDefinitionSearchRequest request = new ActivitySkuDefinitionSearchRequest(
                null, "100001", null, 1, 20);
        ActivitySkuDefinitionSearchQuery query = new ActivitySkuDefinitionSearchQuery(
                null, 100001L, null, new PageRequest(1, 20));
        PageResult<ActivitySkuDefinitionResult> result = PageResult.empty(1, 20);
        PageResult<ActivitySkuDefinitionResponse> expected = PageResult.empty(1, 20);
        when(assembler.toSearchQuery(request)).thenReturn(query);
        when(searchService.search(query)).thenReturn(result);
        when(assembler.toPageResponse(result)).thenReturn(expected);

        // When
        Response<PageResult<ActivitySkuDefinitionResponse>> actual = controller.searchActivitySkuDefinitions(request);

        // Then
        assertEquals(expected, actual.data());
        verifyNoInteractions(adminSessionManager);
    }

    @Test
    void shouldUpdateSkuUsingPathIdentifierAndAuthenticatedOperator() {
        // Given
        UpdateActivitySkuDefinitionRequest request = new UpdateActivitySkuDefinitionRequest(
                "100001", "300001", 600L, 2);
        UpdateActivitySkuDefinitionCommand command = new UpdateActivitySkuDefinitionCommand(
                SKU, 100001L, 300001L, 600L, 2, "admin");
        ActivitySkuDefinitionResult result = mock(ActivitySkuDefinitionResult.class);
        ActivitySkuDefinitionResponse expected = mock(ActivitySkuDefinitionResponse.class);
        stubOperator();
        when(assembler.toUpdateCommand(SKU, request, "admin")).thenReturn(command);
        when(applicationService.updateConfiguration(command)).thenReturn(result);
        when(assembler.toResponse(result)).thenReturn(expected);

        // When
        Response<ActivitySkuDefinitionResponse> actual = controller.updateActivitySkuDefinition(SKU, request);

        // Then
        assertEquals(expected, actual.data());
        verify(applicationService).updateConfiguration(command);
    }

    @Test
    void shouldAdjustStockUsingAuthenticatedOperator() {
        // Given
        AdjustActivitySkuStockRequest request = new AdjustActivitySkuStockRequest(50L, "追加预算", 2);
        AdjustActivitySkuStockCommand command = new AdjustActivitySkuStockCommand(
                SKU, 50L, "追加预算", 2, "admin");
        ActivitySkuDefinitionResult result = mock(ActivitySkuDefinitionResult.class);
        ActivitySkuDefinitionResponse expected = mock(ActivitySkuDefinitionResponse.class);
        stubOperator();
        when(assembler.toAdjustStockCommand(SKU, request, "admin")).thenReturn(command);
        when(applicationService.adjustStock(command)).thenReturn(result);
        when(assembler.toResponse(result)).thenReturn(expected);

        // When
        Response<ActivitySkuDefinitionResponse> actual = controller.adjustActivitySkuStock(SKU, request);

        // Then
        assertEquals(expected, actual.data());
        verify(applicationService).adjustStock(command);
    }

    private void stubOperator() {
        when(adminSessionManager.currentOperator()).thenReturn(
                new AdminOperatorResponse("admin", "系统管理员", Set.of("ROLE_ADMIN")));
    }
}
