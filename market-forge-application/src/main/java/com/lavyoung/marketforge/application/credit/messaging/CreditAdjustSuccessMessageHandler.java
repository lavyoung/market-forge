package com.lavyoung.marketforge.application.credit.messaging;

import com.lavyoung.marketforge.domain.activity.model.entity.DeliveryOrderEntity;
import com.lavyoung.marketforge.domain.activity.service.IRaffleActivityAccountQuotaService;
import com.lavyoung.marketforge.domain.credit.event.CreditAdjustSuccessEvent;
import com.lavyoung.marketforge.types.messaging.MessageContext;
import com.lavyoung.marketforge.types.messaging.MessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 积分账户调额成功消息处理器。
 *
 * <p>该处理器用于承接积分支付成功后的异步发货流程。
 * 当积分账户完成支出调额后，系统发送积分调额成功事件；消费者根据事件中的用户编号
 * 和外部业务幂等号定位待支付活动 SKU 订单，并完成订单发货入账。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditAdjustSuccessMessageHandler implements MessageHandler<CreditAdjustSuccessEvent> {

    private final IRaffleActivityAccountQuotaService raffleActivityAccountQuotaService;

    @Override
    public void handle(CreditAdjustSuccessEvent event, MessageContext context) {
        log.info("收到积分调额成功消息 messageId={} userId={} outBusinessNo={} orderId={} amount={}",
                context.messageId(),
                event.userId(),
                event.outBusinessNo(),
                event.orderId(),
                event.amount());
        raffleActivityAccountQuotaService.updateOrder(new DeliveryOrderEntity(
                event.userId(),
                event.outBusinessNo()
        ));
    }
}
