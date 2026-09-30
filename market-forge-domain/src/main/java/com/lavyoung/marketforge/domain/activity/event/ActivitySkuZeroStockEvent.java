package com.lavyoung.marketforge.domain.activity.event;

import com.lavyoung.marketforge.types.messaging.IntegrationEvent;
import com.lavyoung.marketforge.types.messaging.MqConstants;

import java.time.Instant;
import java.util.UUID;

/**
 * 活动 SKU 零库存集成事件。
 * <p>
 * 活动 SKU 缓存库存耗尽后发出，驱动异步流程清理数据库侧活动 SKU 库存状态。
 *
 * @param eventId    事件唯一标识
 * @param occurredAt 事件发生时间
 * @param sku        活动 SKU
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
public record ActivitySkuZeroStockEvent(
        String eventId,
        Instant occurredAt,
        Long sku
) implements IntegrationEvent {

    /**
     * 业务代码用的便捷构造器：自动生成 eventId 和 occurredAt。
     *
     * @param sku 活动 SKU
     */
    public ActivitySkuZeroStockEvent(Long sku) {
        this(UUID.randomUUID().toString(), Instant.now(), sku);
    }

    @Override
    public String eventType() {
        return MqConstants.SKU_STOCK_ZERO_ROUTING_KEY;
    }
}
