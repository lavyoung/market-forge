package com.lavyoung.marketforge.application.strategy.service;

import com.lavyoung.marketforge.application.strategy.model.StrategyDefinitionSearchItem;
import com.lavyoung.marketforge.application.strategy.model.StrategyDefinitionSearchQuery;
import com.lavyoung.marketforge.application.strategy.query.IStrategyDefinitionReader;
import com.lavyoung.marketforge.application.strategy.service.impl.StrategyDefinitionSearchServiceImpl;
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
 * {@link StrategyDefinitionSearchServiceImpl} 单元测试。
 *
 * <p>验证应用服务只承担查询用例编排，不越过 Reader 直接访问 DAO。</p>
 */
@ExtendWith(MockitoExtension.class)
class StrategyDefinitionSearchServiceImplTest {

    @Mock
    private IStrategyDefinitionReader strategyDefinitionReader;

    @InjectMocks
    private StrategyDefinitionSearchServiceImpl service;

    @Test
    void shouldDelegateValidQueryToReader() {
        // Given
        StrategyDefinitionSearchQuery query = new StrategyDefinitionSearchQuery(100001L, "九宫格策略", "active", new PageRequest(1, 20));
        PageResult<StrategyDefinitionSearchItem> expected = PageResult.empty(1, 20);
        when(strategyDefinitionReader.search(query)).thenReturn(expected);

        // When
        PageResult<StrategyDefinitionSearchItem> actual = service.search(query);

        // Then
        assertEquals(expected, actual);
        verify(strategyDefinitionReader).search(query);
    }

    @Test
    void shouldRejectNullQueryBeforeCallingReader() {
        // When / Then
        assertThrows(NullPointerException.class, () -> service.search(null));
        verifyNoInteractions(strategyDefinitionReader);
    }
}
