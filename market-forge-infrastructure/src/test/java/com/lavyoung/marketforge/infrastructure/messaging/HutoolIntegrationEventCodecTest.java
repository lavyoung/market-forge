package com.lavyoung.marketforge.infrastructure.messaging;

import com.lavyoung.marketforge.domain.award.event.AwardFulfillmentSnapshot;
import com.lavyoung.marketforge.domain.award.event.SendAwardRecordEvent;
import com.lavyoung.marketforge.domain.behavior.event.SendRebateEvent;
import com.lavyoung.marketforge.domain.behavior.model.vo.RebateTypeVO;
import com.lavyoung.marketforge.domain.credit.event.CreditAdjustSuccessEvent;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.messaging.IntegrationEvent;
import com.lavyoung.marketforge.types.messaging.MqConstants;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
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
        AwardFulfillmentSnapshot snapshot = new AwardFulfillmentSnapshot(
                "credit_10", "internal_credit", "automatic", null, "{\"amount\":10}", 1);
        SendAwardRecordEvent expected = SendAwardRecordEvent.builder()
                .eventId("event-001")
                .occurredAt(Instant.parse("2026-10-11T13:30:00Z"))
                .userId("user-001")
                .orderId("order-001")
                .awardId(900901011L)
                .fulfillmentSnapshot(snapshot)
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
                () -> assertEquals(expected.fulfillmentSnapshot(), event.fulfillmentSnapshot()),
                () -> assertEquals(expected.awardTitle(), event.awardTitle())
        );
    }

    /**
     * Given 历史或损坏消息没有履约快照，When 执行补偿反序列化，Then 应报告消息格式错误。
     */
    @Test
    void shouldRejectSendAwardRecordEventWithoutFulfillmentSnapshot() {
        // Given
        HutoolIntegrationEventCodec codec = new HutoolIntegrationEventCodec();
        String messageBody = """
                {
                  "eventId": "event-001",
                  "occurredAt": "2026-10-11T13:30:00Z",
                  "userId": "user-001",
                  "orderId": "order-001",
                  "awardId": 900901011,
                  "awardTitle": "10 积分"
                }
                """;

        // When
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> codec.deserialize(MqConstants.USER_AWARD_SEND_ROUTE_KEY, messageBody));

        // Then
        assertEquals(BusinessResponseCode.MESSAGE_FORMAT_INVALID, exception.getResponseCode());
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

    /**
     * Given 积分调额成功事件，When 经过 JSON 编解码，Then 异步发货所需字段应完整保留。
     */
    @Test
    void shouldPreserveCreditAdjustSuccessFieldsWhenDeserializingCreditAdjustSuccessEvent() {
        // Given
        HutoolIntegrationEventCodec codec = new HutoolIntegrationEventCodec();
        CreditAdjustSuccessEvent expected = CreditAdjustSuccessEvent.builder()
                .eventId("event-003")
                .occurredAt(Instant.parse("2026-10-11T13:40:00Z"))
                .userId("user-001")
                .orderId("credit-order-001")
                .outBusinessNo("exchange-user-001-20261011-001")
                .amount(100L)
                .build();

        // When
        IntegrationEvent actual = codec.deserialize(MqConstants.CREDIT_ADJUST_SUCCESS_ROUTING_KEY, codec.serialize(expected));

        // Then
        assertInstanceOf(CreditAdjustSuccessEvent.class, actual);
        CreditAdjustSuccessEvent event = (CreditAdjustSuccessEvent) actual;
        assertAll(
                () -> assertEquals(expected.eventId(), event.eventId()),
                () -> assertEquals(expected.occurredAt(), event.occurredAt()),
                () -> assertEquals(expected.userId(), event.userId()),
                () -> assertEquals(expected.orderId(), event.orderId()),
                () -> assertEquals(expected.outBusinessNo(), event.outBusinessNo()),
                () -> assertEquals(expected.amount(), event.amount())
        );
    }
}
