package com.lavyoung.marketforge.domain.credit.event;

import com.lavyoung.marketforge.types.messaging.IntegrationEvent;
import com.lavyoung.marketforge.types.messaging.MqConstants;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

/**
 * 积分账户调额成功事件。
 *
 * <p>当积分账户完成一笔调额后，系统通过该事件通知下游继续处理依赖积分支付结果的业务。
 * 在积分兑换活动 SKU 场景中，消费者会根据 {@code userId} 和 {@code outBusinessNo}
 * 定位待支付活动订单，并完成发货入账。</p>
 *
 * @param eventId       事件唯一标识，用于可靠投递和消费幂等
 * @param occurredAt    事件发生时间
 * @param userId        用户编号
 * @param orderId       积分账户流水订单号
 * @param outBusinessNo 外部业务幂等号，对应积分流水的 bizId，也用于关联活动 SKU 订单
 * @param amount        本次调额积分数量
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
@Builder
public record CreditAdjustSuccessEvent(
        String eventId,
        Instant occurredAt,
        String userId,
        String orderId,
        String outBusinessNo,
        Long amount
) implements IntegrationEvent {

    /**
     * 创建积分账户调额成功事件。
     *
     * @param userId        用户编号
     * @param orderId       积分账户流水订单号
     * @param outBusinessNo 外部业务幂等号
     * @param amount        本次调额积分数量
     */
    public CreditAdjustSuccessEvent(String userId, String orderId, String outBusinessNo, Long amount) {
        this(UUID.randomUUID().toString(), Instant.now(), userId, orderId, outBusinessNo, amount);
    }

    @Override
    public String eventType() {
        return MqConstants.CREDIT_ADJUST_SUCCESS_ROUTING_KEY;
    }
}
