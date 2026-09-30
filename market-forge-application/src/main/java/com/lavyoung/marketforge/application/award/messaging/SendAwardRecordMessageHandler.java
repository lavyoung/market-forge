package com.lavyoung.marketforge.application.award.messaging;

import com.lavyoung.marketforge.application.award.service.IAwardApplicationService;
import com.lavyoung.marketforge.domain.award.event.SendAwardRecordEvent;
import com.lavyoung.marketforge.types.messaging.MessageContext;
import com.lavyoung.marketforge.types.messaging.MessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户中奖记录创建事件处理器。
 * <p>
 * 将已持久化的中奖记录幂等推进到待领取状态，为后续按奖品类型执行自动发放或用户领取保留入口。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SendAwardRecordMessageHandler implements MessageHandler<SendAwardRecordEvent> {

    private final IAwardApplicationService awardApplicationService;

    /**
     * 处理用户中奖记录创建事件。
     *
     * @param event   用户中奖记录创建事件
     * @param context 消息上下文
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 中奖记录不存在或状态推进失败时抛出
     */
    @Override
    public void handle(SendAwardRecordEvent event, MessageContext context) {
        log.info("收到用户中奖记录事件 messageId={} userId={} orderId={} awardId={}",
                context.messageId(), event.userId(), event.orderId(), event.awardId());
        awardApplicationService.prepareAwardClaim(event.userId(), event.orderId());
    }
}
