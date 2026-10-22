package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.activity.request.ActivityStageSearchRequest;
import com.lavyoung.marketforge.api.activity.request.CreateActivityStageRequest;
import com.lavyoung.marketforge.api.activity.request.PublishActivityStageRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityStageResponse;
import com.lavyoung.marketforge.api.admin.auth.response.AdminOperatorResponse;
import com.lavyoung.marketforge.application.activity.model.ActivityStageResult;
import com.lavyoung.marketforge.application.activity.model.ActivityStageSearchQuery;
import com.lavyoung.marketforge.application.activity.model.CreateActivityStageCommand;
import com.lavyoung.marketforge.application.activity.model.PublishActivityStageCommand;
import com.lavyoung.marketforge.application.activity.service.IActivityStageApplicationService;
import com.lavyoung.marketforge.application.activity.service.IActivityStageSearchService;
import com.lavyoung.marketforge.trigger.assembler.ActivityStageApiAssembler;
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
 * {@link ActivityStageAdminController} 单元测试。
 *
 * <p>验证后台适配器使用服务端认证身份组装写命令，并把分页查询委托给独立查询服务。</p>
 */
@ExtendWith(MockitoExtension.class)
class ActivityStageAdminControllerTest {

    @Mock
    private IActivityStageApplicationService activityStageApplicationService;

    @Mock
    private IActivityStageSearchService activityStageSearchService;

    @Mock
    private ActivityStageApiAssembler activityStageApiAssembler;

    @Mock
    private AdminSessionManager adminSessionManager;

    @InjectMocks
    private ActivityStageAdminController controller;

    @Test
    void shouldCreateStageWithAuthenticatedOperator() {
        // Given
        CreateActivityStageRequest request = new CreateActivityStageRequest("web", "official", "100301");
        CreateActivityStageCommand command = new CreateActivityStageCommand("web", "official", 100301L, "admin");
        ActivityStageResult result = mock(ActivityStageResult.class);
        ActivityStageResponse expected = mock(ActivityStageResponse.class);
        when(adminSessionManager.currentOperator()).thenReturn(new AdminOperatorResponse("admin", "系统管理员", Set.of("ROLE_ADMIN")));
        when(activityStageApiAssembler.toCreateCommand(request, "admin")).thenReturn(command);
        when(activityStageApplicationService.create(command)).thenReturn(result);
        when(activityStageApiAssembler.toResponse(result)).thenReturn(expected);

        // When
        Response<ActivityStageResponse> actual = controller.createActivityStage(request);

        // Then
        assertEquals(expected, actual.data());
        verify(activityStageApiAssembler).toCreateCommand(request, "admin");
    }

    @Test
    void shouldDelegateSearchWithoutReadingOperatorSession() {
        // Given
        ActivityStageSearchRequest request = new ActivityStageSearchRequest("web", null, "active", 1, 20);
        ActivityStageSearchQuery query = new ActivityStageSearchQuery("web", null, "active", new PageRequest(1, 20));
        PageResult<ActivityStageResult> result = PageResult.empty(1, 20);
        PageResult<ActivityStageResponse> expected = PageResult.empty(1, 20);
        when(activityStageApiAssembler.toSearchQuery(request)).thenReturn(query);
        when(activityStageSearchService.search(query)).thenReturn(result);
        when(activityStageApiAssembler.toPageResponse(result)).thenReturn(expected);

        // When
        Response<PageResult<ActivityStageResponse>> actual = controller.searchActivityStages(request);

        // Then
        assertEquals(expected, actual.data());
        verifyNoInteractions(adminSessionManager);
    }

    @Test
    void shouldPublishWithPathIdVersionAndAuthenticatedOperator() {
        // Given
        Long stageId = 1001L;
        PublishActivityStageRequest request = new PublishActivityStageRequest(2);
        PublishActivityStageCommand command = new PublishActivityStageCommand(stageId, 2, "admin");
        ActivityStageResult result = mock(ActivityStageResult.class);
        ActivityStageResponse expected = mock(ActivityStageResponse.class);
        when(adminSessionManager.currentOperator()).thenReturn(new AdminOperatorResponse("admin", "系统管理员", Set.of("ROLE_ADMIN")));
        when(activityStageApiAssembler.toPublishCommand(stageId, request, "admin")).thenReturn(command);
        when(activityStageApplicationService.publish(command)).thenReturn(result);
        when(activityStageApiAssembler.toResponse(result)).thenReturn(expected);

        // When
        Response<ActivityStageResponse> actual = controller.publishActivityStage(stageId, request);

        // Then
        assertEquals(expected, actual.data());
        verify(activityStageApplicationService).publish(command);
    }
}
