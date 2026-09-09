package com.lavyoung.marketforge.trigger.job;

import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardStockKeyVO;
import com.lavyoung.marketforge.domain.strategy.service.IRaffleStock;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * 验证 {@link StrategyAwardStockJob} 的非阻塞消费、数据库同步和失败重试行为。
 */
class StrategyAwardStockJobTest {

    private static final StrategyAwardStockKeyVO MESSAGE =
            new StrategyAwardStockKeyVO(100_001L, 100_011L);

    /**
     * Given 队列中有一条到期消息，When 执行任务，Then 更新数据库并在队列为空时结束。
     */
    @Test
    void shouldConsumeReadyMessageAndStopOnEmptyQueue() {
        // Given
        IRaffleStock raffleStock = mock(IRaffleStock.class);
        when(raffleStock.pollQueueValue()).thenReturn(Optional.of(MESSAGE), Optional.empty());
        when(raffleStock.updateStrategyAwardStock(MESSAGE.strategyId(), MESSAGE.awardId())).thenReturn(true);

        // When
        new StrategyAwardStockJob(raffleStock).exec();

        // Then
        verify(raffleStock, times(2)).pollQueueValue();
        verify(raffleStock).updateStrategyAwardStock(MESSAGE.strategyId(), MESSAGE.awardId());
        verify(raffleStock, never()).requeueStockUpdate(any());
    }

    /**
     * Given 数据库库存同步抛出运行时异常，When 执行任务，Then 将原消息重新入队。
     */
    @Test
    void shouldRequeueMessageWhenDatabaseUpdateFails() {
        // Given
        IRaffleStock raffleStock = mock(IRaffleStock.class);
        when(raffleStock.pollQueueValue()).thenReturn(Optional.of(MESSAGE), Optional.empty());
        when(raffleStock.updateStrategyAwardStock(MESSAGE.strategyId(), MESSAGE.awardId()))
                .thenThrow(new IllegalStateException("database unavailable"));

        // When
        new StrategyAwardStockJob(raffleStock).exec();

        // Then
        verify(raffleStock).requeueStockUpdate(MESSAGE);
        verify(raffleStock, times(2)).pollQueueValue();
    }
}
