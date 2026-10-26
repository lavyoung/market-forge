package com.lavyoung.marketforge.infrastructure.persistent.query;

import com.lavyoung.marketforge.application.activity.model.ActivityCountDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivityCountDefinitionSearchQuery;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivityCountDefinitionReadModelAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivityCountDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.ActivityCountDefinitionSearchParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityCountPO;
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
import static org.mockito.Mockito.*;

/**
 * 活动次数配置运营读取适配器测试。
 */
@ExtendWith(MockitoExtension.class)
class ActivityCountDefinitionReaderTest {

    @Mock
    private IActivityCountDao activityCountDao;

    @Mock
    private ActivityCountDefinitionReadModelAssembler assembler;

    private ActivityCountDefinitionReader reader;

    @BeforeEach
    void setUp() {
        reader = new ActivityCountDefinitionReader(activityCountDao, assembler);
    }

    @Test
    void shouldReturnEmptyPageWithoutExecutingPageQuery() {
        // Given
        ActivityCountDefinitionSearchQuery query = new ActivityCountDefinitionSearchQuery(
                null, "签到", new PageRequest(1, 20));
        when(activityCountDao.countActivityCountDefinitions(any())).thenReturn(0L);

        // When
        PageResult<ActivityCountDefinitionResult> result = reader.search(query);

        // Then
        assertAll(() -> assertEquals(0L, result.total()), () -> assertEquals(List.of(), result.records()));
        verify(activityCountDao, never()).queryActivityCountDefinitionPage(any());
    }

    @Test
    void shouldPassStablePageWindowAndConvertResults() {
        // Given
        ActivityCountDefinitionSearchQuery query = new ActivityCountDefinitionSearchQuery(
                100301L, "签到", new PageRequest(2, 20));
        ActivityCountPO po = new ActivityCountPO();
        ActivityCountDefinitionResult expected = mock(ActivityCountDefinitionResult.class);
        when(activityCountDao.countActivityCountDefinitions(any())).thenReturn(21L);
        when(activityCountDao.queryActivityCountDefinitionPage(any())).thenReturn(List.of(po));
        when(assembler.toResults(List.of(po))).thenReturn(List.of(expected));

        // When
        PageResult<ActivityCountDefinitionResult> result = reader.search(query);

        // Then
        ArgumentCaptor<ActivityCountDefinitionSearchParam> captor = ArgumentCaptor.forClass(
                ActivityCountDefinitionSearchParam.class);
        verify(activityCountDao).queryActivityCountDefinitionPage(captor.capture());
        assertAll(
                () -> assertEquals(20L, captor.getValue().offset()),
                () -> assertEquals(20, captor.getValue().pageSize()),
                () -> assertEquals(21L, result.total()),
                () -> assertEquals(List.of(expected), result.records())
        );
    }
}
