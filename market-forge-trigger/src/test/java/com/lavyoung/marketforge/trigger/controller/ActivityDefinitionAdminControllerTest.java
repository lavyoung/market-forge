package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.activity.request.ActivityDefinitionSearchRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityDefinitionResponse;
import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionSearchQuery;
import com.lavyoung.marketforge.application.activity.service.IActivityDefinitionSearchService;
import com.lavyoung.marketforge.trigger.assembler.ActivityDefinitionApiAssembler;
import com.lavyoung.marketforge.types.model.PageRequest;
import com.lavyoung.marketforge.types.model.PageResult;
import com.lavyoung.marketforge.types.model.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ActivityDefinitionAdminController} 单元测试。
 *
 * <p>验证 Controller 只完成协议转换和查询用例委托，不访问领域服务或持久化组件。</p>
 */
@ExtendWith(MockitoExtension.class)
class ActivityDefinitionAdminControllerTest {

    @Mock
    private IActivityDefinitionSearchService activityDefinitionSearchService;

    @Mock
    private ActivityDefinitionApiAssembler activityDefinitionApiAssembler;

    @InjectMocks
    private ActivityDefinitionAdminController controller;

    @Test
    void shouldDelegateSearchAndWrapPageResponse() {
        // Given
        ActivityDefinitionSearchRequest request = new ActivityDefinitionSearchRequest(100301L, "九宫格", "open", 1, 20);
        ActivityDefinitionSearchQuery query = new ActivityDefinitionSearchQuery(100301L, "九宫格", "open", new PageRequest(1, 20));
        PageResult<ActivityDefinitionResult> result = PageResult.empty(1, 20);
        PageResult<ActivityDefinitionResponse> expected = PageResult.empty(1, 20);
        when(activityDefinitionApiAssembler.toSearchQuery(request)).thenReturn(query);
        when(activityDefinitionSearchService.search(query)).thenReturn(result);
        when(activityDefinitionApiAssembler.toPageResponse(result)).thenReturn(expected);

        // When
        Response<PageResult<ActivityDefinitionResponse>> actual = controller.searchActivityDefinitions(request);

        // Then
        assertEquals(expected, actual.data());
        verify(activityDefinitionSearchService).search(query);
        verify(activityDefinitionApiAssembler).toPageResponse(result);
    }
}
