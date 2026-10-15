package com.lavyoung.marketforge.domain.award.event;

import com.lavyoung.marketforge.types.messaging.BusinessIdempotentIntegrationEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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
        SendAwardRecordEvent event = new SendAwardRecordEvent(
                "user-001",
                "order-001",
                900901011L,
                "integral",
                "10",
                "10 积分"
        );

        // When / Then
        assertInstanceOf(BusinessIdempotentIntegrationEvent.class, event);
    }
}
