package com.lavyoung.marketforge.trigger.job;

import com.lavyoung.marketforge.domain.activity.model.vo.ActivitySkuStockKeyVO;
import com.lavyoung.marketforge.domain.activity.service.IRaffleActivitySkuStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivitySkuStockJob {

    private final IRaffleActivitySkuStockService skuStock;

    @Scheduled(fixedDelayString = "${market-forge.job.award-stock.fixed-delay-ms:1000}")
    public void exec() {
        try {
            ActivitySkuStockKeyVO activitySkuStockKeyVO = skuStock.takeQueueValue();
            log.info("消费活动SKU库存消息 Redis sku={}, activityId={}", activitySkuStockKeyVO.sku(), activitySkuStockKeyVO.activityId());
            skuStock.updateActivitySkuStock(activitySkuStockKeyVO.sku());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
