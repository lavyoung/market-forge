package com.lavyoung.marketforge.trigger.job;

import com.lavyoung.marketforge.application.strategy.service.IAwardStockService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 验证 {@link StrategyAwardStockJob} 只负责触发库存同步应用用例。
 */
class StrategyAwardStockJobTest {

    /**
     * Given 已注入库存同步应用服务，When 执行定时任务，Then 委托应用用例一次。
     */
    @Test
    void shouldDelegateToApplicationService() {
        // Given
        IAwardStockService applicationService = mock(IAwardStockService.class);

        // When
        new StrategyAwardStockJob(applicationService).exec();

        // Then
        verify(applicationService).synchronizePendingStock();
    }
}
