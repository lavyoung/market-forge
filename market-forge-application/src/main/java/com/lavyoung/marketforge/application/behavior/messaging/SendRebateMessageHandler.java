package com.lavyoung.marketforge.application.behavior.messaging;

import com.lavyoung.marketforge.domain.behavior.event.SendRebateEvent;
import com.lavyoung.marketforge.types.messaging.MessageContext;
import com.lavyoung.marketforge.types.messaging.MessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户行为返利消息处理器。
 *
 * <p>当前章节只验证返利消息可以被可靠投递和消费，因此先记录消息内容。
 * 后续接入积分账户或活动 SKU 充值时，再根据返利类型分发到对应应用服务。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/09
 */
@Slf4j
@Component
public class SendRebateMessageHandler implements MessageHandler<SendRebateEvent> {

    /**
     * 处理用户行为返利发放事件。
     *
     * @param event   用户行为返利发放事件
     * @param context 消息上下文
     */
    @Override
    public void handle(SendRebateEvent event, MessageContext context) {
        log.info("收到用户行为返利消息 messageId={} userId={} bizId={} rebateType={} rebateConfig={}",
                context.messageId(),
                event.userId(),
                event.bizId(),
                event.rebateType(),
                event.rebateConfig());
    }
}
