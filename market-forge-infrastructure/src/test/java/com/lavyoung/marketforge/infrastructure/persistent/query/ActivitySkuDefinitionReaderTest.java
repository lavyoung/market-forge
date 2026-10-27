package com.lavyoung.marketforge.infrastructure.persistent.query;

import com.lavyoung.marketforge.application.activity.model.ActivitySkuDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivitySkuDefinitionSearchQuery;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivitySkuDefinitionReadModelAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivitySkuDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.ActivitySkuDefinitionSearchParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivitySkuPO;
import com.lavyoung.marketforge.types.model.PageRequest;
import com.lavyoung.marketforge.types.model.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 活动 SKU 运营读取适配器测试。
 */
@ExtendWith(MockitoExtension.class)
class ActivitySkuDefinitionReaderTest {

    @Mock
    private IActivitySkuDao activitySkuDao;

    @Mock
    private ActivitySkuDefinitionReadModelAssembler assembler;

    private ActivitySkuDefinitionReader reader;

    @BeforeEach
    void setUp() {
        reader = new ActivitySkuDefinitionReader(activitySkuDao, assembler);
    }

    @Test
    void shouldReturnEmptyPageWithoutExecutingPageQuery() {
        // Given
        ActivitySkuDefinitionSearchQuery query = new ActivitySkuDefinitionSearchQuery(
                null, 100001L, null, new PageRequest(1, 20));
        when(activitySkuDao.countActivitySkuDefinitions(any())).thenReturn(0L);

        // When
        PageResult<ActivitySkuDefinitionResult> result = reader.search(query);

        // Then
        assertAll(() -> assertEquals(0L, result.total()), () -> assertEquals(List.of(), result.records()));
        verify(activitySkuDao, never()).queryActivitySkuDefinitionPage(any());
    }

    @Test
    void shouldPassFiltersAndStablePageWindowToDao() {
        // Given
        ActivitySkuDefinitionSearchQuery query = new ActivitySkuDefinitionSearchQuery(
                900001L, 100001L, 300001L, new PageRequest(2, 20));
        ActivitySkuPO po = new ActivitySkuPO();
        ActivitySkuDefinitionResult expected = mock(ActivitySkuDefinitionResult.class);
        when(activitySkuDao.countActivitySkuDefinitions(any())).thenReturn(21L);
        when(activitySkuDao.queryActivitySkuDefinitionPage(any())).thenReturn(List.of(po));
        when(assembler.toResults(List.of(po))).thenReturn(List.of(expected));

        // When
        PageResult<ActivitySkuDefinitionResult> result = reader.search(query);

        // Then
        ArgumentCaptor<ActivitySkuDefinitionSearchParam> captor =
                ArgumentCaptor.forClass(ActivitySkuDefinitionSearchParam.class);
        verify(activitySkuDao).queryActivitySkuDefinitionPage(captor.capture());
        assertAll(() -> assertEquals(900001L, captor.getValue().sku()),
                () -> assertEquals(100001L, captor.getValue().activityId()),
                () -> assertEquals(300001L, captor.getValue().activityCountId()),
                () -> assertEquals(20L, captor.getValue().offset()),
                () -> assertEquals(20, captor.getValue().pageSize()),
                () -> assertEquals(21L, result.total()),
                () -> assertEquals(List.of(expected), result.records()));
    }
}
