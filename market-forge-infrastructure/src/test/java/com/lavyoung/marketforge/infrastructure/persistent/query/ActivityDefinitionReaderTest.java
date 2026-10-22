package com.lavyoung.marketforge.infrastructure.persistent.query;

import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionSearchQuery;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivityDefinitionReadModelAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivityDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.ActivityDefinitionSearchParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityPO;
import com.lavyoung.marketforge.types.model.PageRequest;
import com.lavyoung.marketforge.types.model.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link ActivityDefinitionReader} 单元测试。
 *
 * <p>覆盖应用查询条件下沉、空结果短路和 DAO 非法空引用，保证数据库异常不会被
 * 伪装为正常空分页。</p>
 */
@ExtendWith(MockitoExtension.class)
class ActivityDefinitionReaderTest {

    @Mock
    private IActivityDao activityDao;

    @Mock
    private ActivityDefinitionReadModelAssembler readModelAssembler;

    @InjectMocks
    private ActivityDefinitionReader reader;

    @Test
    void shouldSearchDefinitionsWithStablePageMetadata() {
        // Given
        ActivityDefinitionSearchQuery query = new ActivityDefinitionSearchQuery(100301L, " 九宫格 ", " open ", new PageRequest(2, 20));
        ActivityPO po = new ActivityPO();
        ActivityDefinitionResult expected = result();
        when(activityDao.countActivityDefinitions(any())).thenReturn(21L);
        when(activityDao.queryActivityDefinitionPage(any())).thenReturn(List.of(po));
        when(readModelAssembler.toResults(List.of(po))).thenReturn(List.of(expected));

        // When
        PageResult<ActivityDefinitionResult> actual = reader.search(query);

        // Then
        ArgumentCaptor<ActivityDefinitionSearchParam> captor = ArgumentCaptor.forClass(ActivityDefinitionSearchParam.class);
        verify(activityDao).countActivityDefinitions(captor.capture());
        verify(activityDao).queryActivityDefinitionPage(captor.capture());
        ActivityDefinitionSearchParam param = captor.getAllValues().get(0);
        assertAll(
                () -> assertEquals(100301L, param.activityId()),
                () -> assertEquals("九宫格", param.activityName()),
                () -> assertEquals("open", param.state()),
                () -> assertEquals(20L, param.offset()),
                () -> assertEquals(20, param.pageSize()),
                () -> assertEquals(param, captor.getAllValues().get(1)),
                () -> assertEquals(List.of(expected), actual.records()),
                () -> assertEquals(21L, actual.total()),
                () -> assertEquals(2, actual.pageNumber()),
                () -> assertEquals(20, actual.pageSize())
        );
    }

    @Test
    void shouldReturnEmptyPageWithoutExecutingPageQueryWhenCountIsZero() {
        // Given
        ActivityDefinitionSearchQuery query = new ActivityDefinitionSearchQuery(null, null, null, new PageRequest(1, 20));
        when(activityDao.countActivityDefinitions(any())).thenReturn(0L);

        // When
        PageResult<ActivityDefinitionResult> actual = reader.search(query);

        // Then
        assertAll(() -> assertEquals(List.of(), actual.records()), () -> assertEquals(0L, actual.total()));
        verify(activityDao, never()).queryActivityDefinitionPage(any());
        verifyNoInteractions(readModelAssembler);
    }

    @Test
    void shouldRejectNullPageResultFromDao() {
        // Given
        ActivityDefinitionSearchQuery query = new ActivityDefinitionSearchQuery(null, null, null, new PageRequest(1, 20));
        when(activityDao.countActivityDefinitions(any())).thenReturn(1L);
        when(activityDao.queryActivityDefinitionPage(any())).thenReturn(null);

        // When / Then
        assertThrows(NullPointerException.class, () -> reader.search(query));
        verifyNoInteractions(readModelAssembler);
    }

    private ActivityDefinitionResult result() {
        return new ActivityDefinitionResult(100301L, "九宫格活动抽奖", "九宫格抽奖活动演示数据", LocalDateTime.of(2026, 1, 1, 0, 0), LocalDateTime.of(2036, 12, 31, 23, 59, 59), 100000, 99999, 100301L, 900901001L, "total", "open", 2, "admin", "operator", LocalDateTime.of(2026, 10, 22, 10, 0), LocalDateTime.of(2026, 10, 22, 11, 0));
    }
}
