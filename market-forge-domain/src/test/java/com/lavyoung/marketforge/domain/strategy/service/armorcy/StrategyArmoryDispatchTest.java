package com.lavyoung.marketforge.domain.strategy.service.armorcy;

import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.impl.StrategyArmoryDispatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 验证 {@link StrategyArmoryDispatch} 的权重策略随机抽奖行为。
 *
 * @author lavyoung
 * @version 1.0.0-SNAPSHOT
 * @email lavyoung1325@outlook.com
 */
@ExtendWith(MockitoExtension.class)
class StrategyArmoryDispatchTest {

    private static final Long STRATEGY_ID = 100_001L;
    private static final String RULE_WEIGHT_VALUE = "4000";
    private static final String EXPECTED_ASSEMBLE_KEY = "100001_4000";
    private static final long EXPECTED_AWARD_ID = 100_011L;
    private static final int RATE_RANGE = 10;

    @Mock
    private IStrategyRepository repository;

    private StrategyArmoryDispatch strategyArmoryDispatch;

    /**
     * Given 模拟仓储，When 初始化测试对象，Then 使用独立的领域服务实例。
     */
    @BeforeEach
    void setUp() {
        strategyArmoryDispatch = new StrategyArmoryDispatch(repository);
    }

    /**
     * Given 已装配的权重概率表，When 按权重抽奖，Then 使用组合键和合法随机下标查询奖品。
     */
    @Test
    void shouldGetRandomAwardIdFromWeightedStrategy() {
        // Given
        when(repository.getRateRange(STRATEGY_ID)).thenReturn(RATE_RANGE);
        when(repository.getStrategyAwardAssemble(eq(EXPECTED_ASSEMBLE_KEY), anyInt()))
                .thenReturn(EXPECTED_AWARD_ID);

        // When
        long awardId = strategyArmoryDispatch.getRandomAwardIdAndWeight(
                STRATEGY_ID,
                RULE_WEIGHT_VALUE
        );

        // Then
        ArgumentCaptor<Integer> rateKeyCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(repository).getStrategyAwardAssemble(
                eq(EXPECTED_ASSEMBLE_KEY),
                rateKeyCaptor.capture()
        );
        assertEquals(EXPECTED_AWARD_ID, awardId);
        assertTrue(rateKeyCaptor.getValue() >= 0);
        assertTrue(rateKeyCaptor.getValue() < RATE_RANGE);
    }

    /**
     * Given 策略尚未装配，When 按权重抽奖，Then 拒绝生成随机下标且不查询奖品表。
     */
    @Test
    void shouldRejectWeightedDrawWhenStrategyIsNotAssembled() {
        // Given
        when(repository.getRateRange(STRATEGY_ID)).thenReturn(0);

        // When
        assertThrows(
                IllegalArgumentException.class,
                () -> strategyArmoryDispatch.getRandomAwardIdAndWeight(
                        STRATEGY_ID,
                        RULE_WEIGHT_VALUE
                )
        );

        // Then
        verify(repository, never()).getStrategyAwardAssemble(anyString(), anyInt());
    }
}
