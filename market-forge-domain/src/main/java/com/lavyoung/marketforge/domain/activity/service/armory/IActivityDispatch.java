package com.lavyoung.marketforge.domain.activity.service.armory;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuStockDeductEntity;

import java.time.LocalDateTime;

/**
 * 活动库存调度端口。
 * <p>
 * 为活动额度责任链提供缓存库存预扣能力，隐藏 Redis 锁、计数器和库存耗尽判断细节。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
public interface IActivityDispatch {

    /**
     * 预扣活动 SKU 缓存库存。
     *
     * @param sku         活动 SKU
     * @param endDateTime 活动结束时间，用于计算库存占位锁过期时间
     * @return 库存预扣结果，包含是否预扣成功和是否进入零库存状态
     * @throws NullPointerException 当活动 SKU 或活动结束时间为空时抛出
     */
    ActivitySkuStockDeductEntity subtractionActivitySkuStock(Long sku, LocalDateTime endDateTime);
}
