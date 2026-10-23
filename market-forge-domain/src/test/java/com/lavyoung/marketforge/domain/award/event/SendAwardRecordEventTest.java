package com.lavyoung.marketforge.domain.award.event;

import com.lavyoung.marketforge.types.messaging.BusinessIdempotentIntegrationEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户中奖记录事件契约测试。
 *
 * <p>中奖发放依赖积分订单唯一键和中奖状态机保证幂等，因此事件必须声明业务幂等契约，
 * 避免消费事务先访问固定库中的通用幂等表，再访问用户所在分片而形成跨库事务。</p>
 *
 * @author lavyoung
 */
class SendAwardRecordEventTest {

    /**
     * 验证中奖记录事件使用业务幂等策略。
     */
    @Test
    void shouldUseBusinessIdempotency() {
        // Given
        AwardFulfillmentSnapshot snapshot = new AwardFulfillmentSnapshot(
                "credit_10", "internal_credit", "automatic", null, "{\"amount\":10}", 1);
        SendAwardRecordEvent event = new SendAwardRecordEvent(
                "user-001",
                "order-001",
                900901011L,
                snapshot,
                "10 积分"
        );

        // When / Then
        assertAll(
                () -> assertInstanceOf(BusinessIdempotentIntegrationEvent.class, event),
                () -> assertSame(snapshot, event.fulfillmentSnapshot()),
                () -> assertEquals("credit_10", event.fulfillmentSnapshot().awardCode()),
                () -> assertEquals("internal_credit", event.fulfillmentSnapshot().awardType()),
                () -> assertEquals("{\"amount\":10}", event.fulfillmentSnapshot().configContent()),
                () -> assertEquals(1, event.fulfillmentSnapshot().configVersion())
        );
    }

    /**
     * Given 履约快照缺失，When 创建中奖记录事件，Then 应拒绝不完整事件进入可靠消息表。
     */
    @Test
    void shouldRejectMissingFulfillmentSnapshot() {
        // Given / When / Then
        assertThrows(NullPointerException.class,
                () -> new SendAwardRecordEvent("user-001", "order-001", 900901011L, null, "10 积分"));
    }
}
