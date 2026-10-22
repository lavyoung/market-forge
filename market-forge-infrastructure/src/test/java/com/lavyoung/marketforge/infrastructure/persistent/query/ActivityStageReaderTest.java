package com.lavyoung.marketforge.infrastructure.persistent.query;

import com.lavyoung.marketforge.application.activity.model.ActivityStageResult;
import com.lavyoung.marketforge.application.activity.model.ActivityStageSearchQuery;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivityStageReadModelAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivityStageDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.ActivityStageSearchParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityStagePO;
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
import static org.mockito.Mockito.*;

/**
 * {@link ActivityStageReader} 单元测试。
 *
 * <p>覆盖分页参数下沉、空结果短路和数据库返回空引用等边界，防止后台列表引入
 * 无意义查询或把基础设施异常伪装成正常空页。</p>
 */
@ExtendWith(MockitoExtension.class)
class ActivityStageReaderTest {

    @Mock
    private IActivityStageDao activityStageDao;

    @Mock
    private ActivityStageReadModelAssembler readModelAssembler;

    @InjectMocks
    private ActivityStageReader reader;

    @Test
    void shouldSearchStagesWithNormalizedFiltersAndStablePageMetadata() {
        // Given
        ActivityStageSearchQuery query = new ActivityStageSearchQuery(" web ", " official ", " active ", new PageRequest(2, 20));
        ActivityStagePO po = new ActivityStagePO();
        ActivityStageResult expected = result();
        when(activityStageDao.countActivityStages(any())).thenReturn(21L);
        when(activityStageDao.queryActivityStagePage(any())).thenReturn(List.of(po));
        when(readModelAssembler.toResults(List.of(po))).thenReturn(List.of(expected));

        // When
        PageResult<ActivityStageResult> actual = reader.search(query);

        // Then
        ArgumentCaptor<ActivityStageSearchParam> captor = ArgumentCaptor.forClass(ActivityStageSearchParam.class);
        verify(activityStageDao).countActivityStages(captor.capture());
        verify(activityStageDao).queryActivityStagePage(captor.capture());
        ActivityStageSearchParam param = captor.getAllValues().get(0);
        assertAll(
                () -> assertEquals("web", param.channel()),
                () -> assertEquals("official", param.source()),
                () -> assertEquals("active", param.state()),
                () -> assertEquals(20L, param.offset()),
                () -> assertEquals(20, param.pageSize()),
                () -> assertEquals(param, captor.getAllValues().get(1)),
                () -> assertEquals(List.of(expected), actual.records()),
                () -> assertEquals(21L, actual.total()),
                () -> assertEquals(2, actual.pageNumber())
        );
    }

    @Test
    void shouldReturnEmptyPageWithoutExecutingPageQueryWhenCountIsZero() {
        // Given
        ActivityStageSearchQuery query = new ActivityStageSearchQuery(null, null, null, new PageRequest(1, 20));
        when(activityStageDao.countActivityStages(any())).thenReturn(0L);

        // When
        PageResult<ActivityStageResult> actual = reader.search(query);

        // Then
        assertAll(() -> assertEquals(List.of(), actual.records()), () -> assertEquals(0L, actual.total()));
        verify(activityStageDao, never()).queryActivityStagePage(any());
        verifyNoInteractions(readModelAssembler);
    }

    @Test
    void shouldRejectNullPageResultFromDao() {
        // Given
        ActivityStageSearchQuery query = new ActivityStageSearchQuery(null, null, null, new PageRequest(1, 20));
        when(activityStageDao.countActivityStages(any())).thenReturn(1L);
        when(activityStageDao.queryActivityStagePage(any())).thenReturn(null);

        // When / Then
        assertThrows(NullPointerException.class, () -> reader.search(query));
        verifyNoInteractions(readModelAssembler);
    }

    private ActivityStageResult result() {
        return new ActivityStageResult(9_007_199_254_740_993L, "web", "official", 9_007_199_254_740_994L, "active", 2, "admin", "admin", LocalDateTime.of(2026, 10, 22, 10, 0), LocalDateTime.of(2026, 10, 22, 10, 30));
    }
}
