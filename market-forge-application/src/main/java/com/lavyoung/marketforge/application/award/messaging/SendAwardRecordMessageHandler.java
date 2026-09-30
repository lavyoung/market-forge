package com.lavyoung.marketforge.application.award.messaging;

import com.lavyoung.marketforge.domain.award.event.SendAwardRecordEvent;
import com.lavyoung.marketforge.types.messaging.MessageContext;
import com.lavyoung.marketforge.types.messaging.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户中奖记录创建事件处理器。
 * <p>
 * 当前章节只负责确认中奖记录事件已被可靠消费并记录日志。后续接入按奖品类型发放或用户领取时，
 * 再由发奖应用用例根据奖品类型推进中奖记录状态。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Slf4j
@Component
public class SendAwardRecordMessageHandler implements MessageHandler<SendAwardRecordEvent> {

    /**
     * 处理用户中奖记录创建事件。
     *
     * @param event   用户中奖记录创建事件
     * @param context 消息上下文
     */
    @Override
    public void handle(SendAwardRecordEvent event, MessageContext context) {
        log.info("收到用户中奖记录事件 messageId={} userId={} orderId={} awardId={}",
                context.messageId(), event.userId(), event.orderId(), event.awardId());
    }
}
