package com.lavyoung.marketforge.domain.strategy.event;

import com.lavyoung.marketforge.types.messaging.IntegrationEvent;
import com.lavyoung.marketforge.types.messaging.MqConstants;

import java.time.Instant;
import java.util.UUID;

/**
 *
 * 奖品库存扣减集成事件：抽奖命中奖品后发出，驱动库存异步扣减。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
public record AwardStockDeductedEvent(
        String eventId,
        Instant occurredAt,
        Long strategyId,
        Long awardId,
        String userId
) implements IntegrationEvent {

    /**
     * 业务代码用的便捷构造器：自动生成 eventId 和 occurredAt。
     */
    public AwardStockDeductedEvent(Long strategyId, Long awardId, String userId) {
        this(UUID.randomUUID().toString(), Instant.now(), strategyId, awardId, userId);
    }

    @Override
    public String eventType() {
        return MqConstants.AWARD_STOCK_DEDUCT_ROUTING_KEY;
    }
}
