package com.lavyoung.marketforge.application.activity.service;

import com.lavyoung.marketforge.application.activity.model.ActivitySkuDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivitySkuDefinitionSearchQuery;
import com.lavyoung.marketforge.application.activity.query.IActivitySkuDefinitionReader;
import com.lavyoung.marketforge.application.activity.service.impl.ActivitySkuDefinitionSearchServiceImpl;
import com.lavyoung.marketforge.types.model.PageRequest;
import com.lavyoung.marketforge.types.model.PageResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * 活动 SKU 查询应用服务测试。
 */
class ActivitySkuDefinitionSearchServiceImplTest {

    @Test
    void shouldDelegateValidatedQueryToReader() {
        // Given
        IActivitySkuDefinitionReader reader = mock(IActivitySkuDefinitionReader.class);
        ActivitySkuDefinitionSearchServiceImpl service = new ActivitySkuDefinitionSearchServiceImpl(reader);
        ActivitySkuDefinitionSearchQuery query = new ActivitySkuDefinitionSearchQuery(
                900001L, 100001L, 300001L, new PageRequest(1, 20));
        PageResult<ActivitySkuDefinitionResult> expected = PageResult.empty(1, 20);
        when(reader.search(query)).thenReturn(expected);

        // When
        PageResult<ActivitySkuDefinitionResult> actual = service.search(query);

        // Then
        assertEquals(expected, actual);
        verify(reader).search(query);
    }

    @Test
    void shouldRejectMissingQuery() {
        IActivitySkuDefinitionReader reader = mock(IActivitySkuDefinitionReader.class);
        ActivitySkuDefinitionSearchServiceImpl service = new ActivitySkuDefinitionSearchServiceImpl(reader);
        assertThrows(NullPointerException.class, () -> service.search(null));
    }
}
