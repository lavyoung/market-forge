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

    /**
     * 执行活动 SKU 库存同步任务。
     * <p>
     * 每次调度从延迟队列中取出一条库存同步消息，队列为空时直接返回；取到消息后触发领域服务
     * 扣减数据库库存。异常会向外抛出，交由调度框架记录本次执行失败。
     *
     * @throws RuntimeException 当队列读取或库存同步失败时抛出
     */
    @Scheduled(fixedDelayString = "${market-forge.job.award-stock.fixed-delay-ms:1000}")
    public void exec() {
        try {
            ActivitySkuStockKeyVO activitySkuStockKeyVO = skuStock.takeQueueValue();
            if (activitySkuStockKeyVO == null) {
                return;
            }
            log.info("消费活动SKU库存消息 Redis sku={}, activityId={}", activitySkuStockKeyVO.sku(), activitySkuStockKeyVO.activityId());
            skuStock.updateActivitySkuStock(activitySkuStockKeyVO.sku());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
