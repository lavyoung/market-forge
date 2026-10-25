package com.lavyoung.marketforge.infrastructure.persistent.query;

import com.lavyoung.marketforge.application.strategy.model.StrategyDefinitionSearchItem;
import com.lavyoung.marketforge.application.strategy.model.StrategyDefinitionSearchQuery;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.StrategyDefinitionReadModelAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IStrategyDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.StrategyDefinitionSearchParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyPO;
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
 * {@link StrategyDefinitionReader} 单元测试。
 *
 * <p>覆盖查询条件下沉、空结果短路和 DAO 非法空引用，确保分页查询只读取策略主表，
 * 且数据库异常不会被伪装成正常空分页。</p>
 */
@ExtendWith(MockitoExtension.class)
class StrategyDefinitionReaderTest {

    @Mock
    private IStrategyDao strategyDao;

    @Mock
    private StrategyDefinitionReadModelAssembler readModelAssembler;

    @InjectMocks
    private StrategyDefinitionReader reader;

    @Test
    void shouldSearchDefinitionsWithStablePageMetadata() {
        // Given
        StrategyDefinitionSearchQuery query = new StrategyDefinitionSearchQuery(100001L, " 九宫格 ", " active ", new PageRequest(2, 20));
        StrategyPO po = new StrategyPO();
        StrategyDefinitionSearchItem expected = searchItem();
        when(strategyDao.countStrategyDefinitions(any())).thenReturn(21L);
        when(strategyDao.queryStrategyDefinitionPage(any())).thenReturn(List.of(po));
        when(readModelAssembler.toItems(List.of(po))).thenReturn(List.of(expected));

        // When
        PageResult<StrategyDefinitionSearchItem> actual = reader.search(query);

        // Then
        ArgumentCaptor<StrategyDefinitionSearchParam> captor = ArgumentCaptor.forClass(StrategyDefinitionSearchParam.class);
        verify(strategyDao).countStrategyDefinitions(captor.capture());
        verify(strategyDao).queryStrategyDefinitionPage(captor.capture());
        StrategyDefinitionSearchParam param = captor.getAllValues().get(0);
        assertAll(
                () -> assertEquals(100001L, param.strategyId()),
                () -> assertEquals("九宫格", param.strategyDesc()),
                () -> assertEquals("active", param.state()),
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
        StrategyDefinitionSearchQuery query = new StrategyDefinitionSearchQuery(null, null, null, new PageRequest(1, 20));
        when(strategyDao.countStrategyDefinitions(any())).thenReturn(0L);

        // When
        PageResult<StrategyDefinitionSearchItem> actual = reader.search(query);

        // Then
        assertAll(() -> assertEquals(List.of(), actual.records()), () -> assertEquals(0L, actual.total()));
        verify(strategyDao, never()).queryStrategyDefinitionPage(any());
        verifyNoInteractions(readModelAssembler);
    }

    @Test
    void shouldRejectNullPageResultFromDao() {
        // Given
        StrategyDefinitionSearchQuery query = new StrategyDefinitionSearchQuery(null, null, null, new PageRequest(1, 20));
        when(strategyDao.countStrategyDefinitions(any())).thenReturn(1L);
        when(strategyDao.queryStrategyDefinitionPage(any())).thenReturn(null);

        // When / Then
        assertThrows(NullPointerException.class, () -> reader.search(query));
        verifyNoInteractions(readModelAssembler);
    }

    private StrategyDefinitionSearchItem searchItem() {
        return new StrategyDefinitionSearchItem(100001L, "九宫格抽奖策略", "rule_blacklist,rule_weight", "active", 2, "admin", "operator", LocalDateTime.of(2026, 10, 25, 10, 0), LocalDateTime.of(2026, 10, 25, 11, 0));
    }
}
