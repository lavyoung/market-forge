package com.lavyoung.marketforge.trigger.job;

import com.lavyoung.marketforge.application.activity.service.IActivitySkuStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 活动 SKU 库存异步同步调度适配器。
 * <p>
 * 周期性消费待同步库存消息并触发库存扣减用例，不在调度适配器中承载库存计算逻辑。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/08
 */
@Component
@RequiredArgsConstructor
public class ActivitySkuStockJob {

    private final IActivitySkuStockService activitySkuStockService;

    /**
     * 执行活动 SKU 库存同步任务。
     * <p>
     * 仅触发应用层库存同步用例；消息读取、异常包装和库存扣减由 application 层处理。
     * 异常会向外抛出，交由调度框架记录本次执行失败。
     *
     * @throws RuntimeException 当队列读取或库存同步失败时抛出
     */
    @Scheduled(fixedDelayString = "${market-forge.job.award-stock.fixed-delay-ms:1000}")
    public void exec() {
        activitySkuStockService.synchronizePendingStock();
    }
}
