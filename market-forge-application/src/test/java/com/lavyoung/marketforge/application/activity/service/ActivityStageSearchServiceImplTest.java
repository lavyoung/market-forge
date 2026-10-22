package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityStageResult;
import com.lavyoung.marketforge.application.activity.model.ActivityStageSearchQuery;
import com.lavyoung.marketforge.application.activity.query.IActivityStageReader;
import com.lavyoung.marketforge.application.activity.service.impl.ActivityStageSearchServiceImpl;
import com.lavyoung.marketforge.types.model.PageRequest;
import com.lavyoung.marketforge.types.model.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * {@link ActivityStageSearchServiceImpl} 单元测试。
 *
 * <p>验证应用服务只声明查询边界并委托 Reader，不在应用层构造 SQL 参数。</p>
 */
@ExtendWith(MockitoExtension.class)
class ActivityStageSearchServiceImplTest {

    @Mock
    private IActivityStageReader activityStageReader;

    @InjectMocks
    private ActivityStageSearchServiceImpl service;

    @Test
    void shouldDelegateValidQueryToReader() {
        // Given
        ActivityStageSearchQuery query = new ActivityStageSearchQuery("web", "official", "active", new PageRequest(1, 20));
        PageResult<ActivityStageResult> expected = PageResult.empty(1, 20);
        when(activityStageReader.search(query)).thenReturn(expected);

        // When
        PageResult<ActivityStageResult> actual = service.search(query);

        // Then
        assertEquals(expected, actual);
        verify(activityStageReader).search(query);
    }

    @Test
    void shouldRejectNullQueryBeforeCallingReader() {
        // When / Then
        assertThrows(NullPointerException.class, () -> service.search(null));
        verifyNoInteractions(activityStageReader);
    }
}
