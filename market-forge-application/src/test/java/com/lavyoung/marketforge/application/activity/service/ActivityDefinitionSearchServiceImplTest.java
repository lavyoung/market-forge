package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionSearchQuery;
import com.lavyoung.marketforge.application.activity.query.IActivityDefinitionReader;
import com.lavyoung.marketforge.application.activity.service.impl.ActivityDefinitionSearchServiceImpl;
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
 * {@link ActivityDefinitionSearchServiceImpl} 单元测试。
 *
 * <p>验证应用服务保持查询编排职责，只校验用例输入并委托活动定义 Reader。</p>
 */
@ExtendWith(MockitoExtension.class)
class ActivityDefinitionSearchServiceImplTest {

    @Mock
    private IActivityDefinitionReader activityDefinitionReader;

    @InjectMocks
    private ActivityDefinitionSearchServiceImpl service;

    @Test
    void shouldDelegateValidQueryToReader() {
        // Given
        ActivityDefinitionSearchQuery query = new ActivityDefinitionSearchQuery(100301L, "九宫格活动", "open", new PageRequest(1, 20));
        PageResult<ActivityDefinitionResult> expected = PageResult.empty(1, 20);
        when(activityDefinitionReader.search(query)).thenReturn(expected);

        // When
        PageResult<ActivityDefinitionResult> actual = service.search(query);

        // Then
        assertEquals(expected, actual);
        verify(activityDefinitionReader).search(query);
    }

    @Test
    void shouldRejectNullQueryBeforeCallingReader() {
        // When / Then
        assertThrows(NullPointerException.class, () -> service.search(null));
        verifyNoInteractions(activityDefinitionReader);
    }
}
