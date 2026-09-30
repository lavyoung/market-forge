package com.lavyoung.marketforge.trigger.job;

import com.lavyoung.marketforge.application.activity.service.IActivitySkuStockService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 验证 {@link ActivitySkuStockJob} 只负责触发活动库存同步应用用例。
 */
class ActivitySkuStockJobTest {

    /**
     * Given 已注入活动库存同步应用服务，When 执行定时任务，Then 委托应用用例一次。
     */
    @Test
    void shouldDelegateToApplicationService() {
        // Given
        IActivitySkuStockService applicationService = mock(IActivitySkuStockService.class);

        // When
        new ActivitySkuStockJob(applicationService).exec();

        // Then
        verify(applicationService).synchronizePendingStock();
    }
}
