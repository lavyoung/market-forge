package com.lavyoung.marketforge.application.activity.service.impl;

import com.lavyoung.marketforge.application.activity.service.IActivitySkuStockService;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivitySkuStockKeyVO;
import com.lavyoung.marketforge.domain.activity.service.IRaffleActivitySkuStockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 活动 SKU 库存异步同步用例实现。
 * <p>
 * 从活动库存补偿队列读取一条到期消息，并委托领域服务扣减数据库库存。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivitySkuStockServiceImpl implements IActivitySkuStockService {

    private final IRaffleActivitySkuStockService skuStock;

    /**
     * {@inheritDoc}
     */
    @Override
    public void synchronizePendingStock() {
        try {
            ActivitySkuStockKeyVO activitySkuStockKeyVO = skuStock.takeQueueValue();
            if (activitySkuStockKeyVO == null) {
                return;
            }
            log.info("消费活动SKU库存消息 Redis sku={}, activityId={}",
                    activitySkuStockKeyVO.sku(), activitySkuStockKeyVO.activityId());
            skuStock.updateActivitySkuStock(activitySkuStockKeyVO.sku());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
