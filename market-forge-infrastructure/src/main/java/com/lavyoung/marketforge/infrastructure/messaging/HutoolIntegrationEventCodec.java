package com.lavyoung.marketforge.infrastructure.messaging;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.lavyoung.marketforge.domain.award.event.SendAwardRecordEvent;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.messaging.IntegrationEvent;
import com.lavyoung.marketforge.types.messaging.IntegrationEventCodec;
import com.lavyoung.marketforge.types.messaging.MqConstants;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 基于 Hutool JSON 的集成事件编解码器。
 * <p>
 * 本地消息任务表只保存事件类型和 JSON 字符串，投递补偿时通过该组件还原为运行期事件对象。
 * 新增可靠消息事件时，应在 {@link #deserialize(String, String)} 中显式注册事件类型。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/30
 */
@Component
@Slf4j
public class HutoolIntegrationEventCodec implements IntegrationEventCodec {

    /**
     * {@inheritDoc}
     */
    @Override
    public String serialize(IntegrationEvent event) {
        return JSONUtil.toJsonStr(event);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IntegrationEvent deserialize(String eventType, String messageBody) {
        return switch (eventType) {
            case MqConstants.USER_AWARD_SEND_ROUTE_KEY -> deserializeSendAwardRecordEvent(messageBody);
            default -> throw BusinessException.of(BusinessResponseCode.MESSAGE_FORMAT_INVALID, eventType);
        };
    }

    private SendAwardRecordEvent deserializeSendAwardRecordEvent(String messageBody) {
        JSONObject jsonObject = JSONUtil.parseObj(messageBody);
        return SendAwardRecordEvent.builder()
                .eventId(jsonObject.getStr("eventId"))
                .occurredAt(parseOccurredAt(jsonObject.getStr("occurredAt")))
                .userId(jsonObject.getStr("userId"))
                .orderId(jsonObject.getStr("orderId"))
                .awardId(jsonObject.getLong("awardId"))
                .awardTitle(jsonObject.getStr("awardTitle"))
                .build();
    }

    private Instant parseOccurredAt(String occurredAt) {
        if (occurredAt.chars().allMatch(Character::isDigit)) {
            return Instant.ofEpochMilli(Long.parseLong(occurredAt));
        }
        return Instant.parse(occurredAt);
    }
}
