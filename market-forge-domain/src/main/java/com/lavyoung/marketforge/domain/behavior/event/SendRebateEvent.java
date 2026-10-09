package com.lavyoung.marketforge.domain.behavior.event;

import com.lavyoung.marketforge.domain.behavior.model.vo.RebateTypeVO;
import com.lavyoung.marketforge.types.messaging.IntegrationEvent;
import com.lavyoung.marketforge.types.messaging.MqConstants;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * 用户行为返利发放事件。
 *
 * <p>当用户行为返利订单创建成功后，系统通过该事件通知下游完成具体发放。
 * 当前事件只表达“需要发放一笔返利”的事实，不直接执行积分入账或 SKU 充值。</p>
 *
 * @param eventId      事件唯一标识，用于可靠投递和消费幂等
 * @param occurredAt   事件发生时间
 * @param userId       用户标识
 * @param bizId        下游发放幂等标识
 * @param rebateType   返利类型
 * @param rebateConfig 返利配置值
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/08
 */
@Builder
public record SendRebateEvent(
        String eventId,
        Instant occurredAt,
        String userId,
        String bizId,
        RebateTypeVO rebateType,
        String rebateConfig
) implements IntegrationEvent {

    /**
     * 创建用户行为返利发放事件。
     *
     * @param userId       用户标识
     * @param bizId        下游发放幂等标识
     * @param rebateType   返利类型
     * @param rebateConfig 返利配置值
     */
    public SendRebateEvent(String userId, String bizId, RebateTypeVO rebateType, String rebateConfig) {
        this(UUID.randomUUID().toString(), Instant.now(), userId, bizId, rebateType, rebateConfig);
    }

    @Override
    public String eventType() {
        return MqConstants.SEND_REBATE_ROUTING_KEY;
    }
}
