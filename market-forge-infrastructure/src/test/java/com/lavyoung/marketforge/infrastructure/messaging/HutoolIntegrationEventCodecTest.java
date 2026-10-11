package com.lavyoung.marketforge.infrastructure.messaging;

import com.lavyoung.marketforge.domain.award.event.SendAwardRecordEvent;
import com.lavyoung.marketforge.domain.behavior.event.SendRebateEvent;
import com.lavyoung.marketforge.domain.behavior.model.vo.RebateTypeVO;
import com.lavyoung.marketforge.types.messaging.IntegrationEvent;
import com.lavyoung.marketforge.types.messaging.MqConstants;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证本地消息任务中的事件 JSON 编解码契约。
 *
 * <p>可靠消息补偿投递会从 task 表读取 {@code event_type} 和 {@code message_body}，
 * 再通过该 codec 还原成运行期事件对象。这里必须覆盖新增字段，避免即时投递正常、
 * 补偿重投时丢业务参数。</p>
 */
class HutoolIntegrationEventCodecTest {

    /**
     * Given 用户中奖记录事件携带积分奖品配置，When 经过 JSON 编解码，Then 发奖关键字段应完整保留。
     */
    @Test
    void shouldPreserveAwardGrantFieldsWhenDeserializingSendAwardRecordEvent() {
        // Given
        HutoolIntegrationEventCodec codec = new HutoolIntegrationEventCodec();
        SendAwardRecordEvent expected = SendAwardRecordEvent.builder()
                .eventId("event-001")
                .occurredAt(Instant.parse("2026-10-11T13:30:00Z"))
                .userId("user-001")
                .orderId("order-001")
                .awardId(900901011L)
                .awardKey("integral")
                .awardConfig("10")
                .awardTitle("10 积分")
                .build();

        // When
        IntegrationEvent actual = codec.deserialize(MqConstants.USER_AWARD_SEND_ROUTE_KEY, codec.serialize(expected));

        // Then
        assertInstanceOf(SendAwardRecordEvent.class, actual);
        SendAwardRecordEvent event = (SendAwardRecordEvent) actual;
        assertAll(
                () -> assertEquals(expected.eventId(), event.eventId()),
                () -> assertEquals(expected.occurredAt(), event.occurredAt()),
                () -> assertEquals(expected.userId(), event.userId()),
                () -> assertEquals(expected.orderId(), event.orderId()),
                () -> assertEquals(expected.awardId(), event.awardId()),
                () -> assertEquals(expected.awardKey(), event.awardKey()),
                () -> assertEquals(expected.awardConfig(), event.awardConfig()),
                () -> assertEquals(expected.awardTitle(), event.awardTitle())
        );
    }

    /**
     * Given 用户行为积分返利事件，When 经过 JSON 编解码，Then 返利类型和配置应完整保留。
     */
    @Test
    void shouldPreserveRebateFieldsWhenDeserializingSendRebateEvent() {
        // Given
        HutoolIntegrationEventCodec codec = new HutoolIntegrationEventCodec();
        SendRebateEvent expected = SendRebateEvent.builder()
                .eventId("event-002")
                .occurredAt(Instant.parse("2026-10-11T13:35:00Z"))
                .userId("user-001")
                .bizId("user-001_integral_sign-20261011-user-001")
                .rebateType(RebateTypeVO.INTEGRAL)
                .rebateConfig("10")
                .build();

        // When
        IntegrationEvent actual = codec.deserialize(MqConstants.SEND_REBATE_ROUTING_KEY, codec.serialize(expected));

        // Then
        assertInstanceOf(SendRebateEvent.class, actual);
        SendRebateEvent event = (SendRebateEvent) actual;
        assertAll(
                () -> assertEquals(expected.eventId(), event.eventId()),
                () -> assertEquals(expected.occurredAt(), event.occurredAt()),
                () -> assertEquals(expected.userId(), event.userId()),
                () -> assertEquals(expected.bizId(), event.bizId()),
                () -> assertEquals(expected.rebateType(), event.rebateType()),
                () -> assertEquals(expected.rebateConfig(), event.rebateConfig())
        );
    }
}
