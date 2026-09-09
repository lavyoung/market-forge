package com.lavyoung.marketforge.trigger.job;

import com.lavyoung.marketforge.application.strategy.service.IAwardStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 奖品库存异步同步调度适配器。
 * <p>
 * 周期性触发库存同步应用用例，不在调度适配器中承载消息消费与失败重试逻辑。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/08
 */
@Component
@RequiredArgsConstructor
public class StrategyAwardStockJob {

    /**
     * 奖品库存同步应用服务。
     */
    private final IAwardStockService awardStockApplicationService;

    /**
     * 触发当前已到期库存消息的批量同步。
     * 默认每秒执行一次，可通过 {@code market-forge.job.award-stock.fixed-delay-ms} 覆盖调度间隔。
     */
    @Scheduled(fixedDelayString = "${market-forge.job.award-stock.fixed-delay-ms:1000}")
    public void exec() {
        awardStockApplicationService.synchronizePendingStock();
    }
}
