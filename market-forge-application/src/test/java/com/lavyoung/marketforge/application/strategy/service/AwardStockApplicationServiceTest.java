package com.lavyoung.marketforge.application.strategy.service;

import com.lavyoung.marketforge.application.strategy.service.impl.AwardStockServiceImpl;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardStockKeyVO;
import com.lavyoung.marketforge.domain.strategy.service.IRaffleStock;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * 验证库存同步应用用例的批处理和失败重试行为。
 */
class AwardStockApplicationServiceTest {

    private static final StrategyAwardStockKeyVO MESSAGE =
            new StrategyAwardStockKeyVO(100_001L, 100_011L);

    /**
     * Given 队列中有到期消息，When 同步库存，Then 更新数据库并在队列为空时结束。
     */
    @Test
    void shouldSynchronizeReadyMessageAndStopOnEmptyQueue() {
        // Given
        IRaffleStock raffleStock = mock(IRaffleStock.class);
        when(raffleStock.pollQueueValue()).thenReturn(Optional.of(MESSAGE), Optional.empty());
        when(raffleStock.updateStrategyAwardStock(MESSAGE.strategyId(), MESSAGE.awardId())).thenReturn(true);

        // When
        new AwardStockServiceImpl(raffleStock).synchronizePendingStock();

        // Then
        verify(raffleStock, times(2)).pollQueueValue();
        verify(raffleStock).updateStrategyAwardStock(MESSAGE.strategyId(), MESSAGE.awardId());
        verify(raffleStock, never()).requeueStockUpdate(any());
    }

    /**
     * Given 数据库同步抛出异常，When 同步库存，Then 将原消息重新入队并继续消费。
     */
    @Test
    void shouldRequeueMessageWhenSynchronizationFails() {
        // Given
        IRaffleStock raffleStock = mock(IRaffleStock.class);
        when(raffleStock.pollQueueValue()).thenReturn(Optional.of(MESSAGE), Optional.empty());
        when(raffleStock.updateStrategyAwardStock(MESSAGE.strategyId(), MESSAGE.awardId()))
                .thenThrow(new IllegalStateException("database unavailable"));

        // When
        new AwardStockServiceImpl(raffleStock).synchronizePendingStock();

        // Then
        verify(raffleStock).requeueStockUpdate(MESSAGE);
        verify(raffleStock, times(2)).pollQueueValue();
    }
}
