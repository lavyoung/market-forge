package com.lavyoung.marketforge.domain.activity.event;

import com.lavyoung.marketforge.types.messaging.IntegrationEvent;
import com.lavyoung.marketforge.types.messaging.MqConstants;

import java.time.Instant;
import java.util.UUID;

/**
 * 活动 SKU 库存扣减集成事件。
 * <p>
 * Redis 缓存库存预扣成功后发出，驱动异步流程扣减数据库侧活动 SKU 库存。
 *
 * @param eventId    事件唯一标识
 * @param occurredAt 事件发生时间
 * @param sku        活动 SKU
 * @param activityId 活动标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
public record ActivitySkuStockDeductedEvent(
        String eventId,
        Instant occurredAt,
        Long sku,
        Long activityId
) implements IntegrationEvent {

    /**
     * 业务代码用的便捷构造器：自动生成 eventId 和 occurredAt。
     *
     * @param sku        活动 SKU
     * @param activityId 活动标识
     */
    public ActivitySkuStockDeductedEvent(Long sku, Long activityId) {
        this(UUID.randomUUID().toString(), Instant.now(), sku, activityId);
    }

    @Override
    public String eventType() {
        return MqConstants.SKU_STOCK_DEDUCT_ROUTING_KEY;
    }
}
