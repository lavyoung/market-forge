package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.application.activity.service.IActivityStageApplicationService;
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
 * {@link ActivityStageController} 单元测试。
 *
 * <p>验证用户端接口只解析当前活动，并以字符串响应保护 JavaScript 整数精度。</p>
 */
@ExtendWith(MockitoExtension.class)
class ActivityStageControllerTest {

    @Mock
    private IActivityStageApplicationService activityStageApplicationService;

    @InjectMocks
    private ActivityStageController controller;

    @Test
    void shouldReturnActiveActivityIdAsString() {
        // Given
        Long activityId = 9_007_199_254_740_993L;
        when(activityStageApplicationService.queryActiveActivityId("web", "official")).thenReturn(activityId);

        // When
        Response<?> response = controller.queryActiveActivity("web", "official");

        // Then
        assertEquals(activityId.toString(), ((com.lavyoung.marketforge.api.activity.response.ActiveActivityStageResponse) response.data()).activityId());
        verify(activityStageApplicationService).queryActiveActivityId("web", "official");
    }
}
