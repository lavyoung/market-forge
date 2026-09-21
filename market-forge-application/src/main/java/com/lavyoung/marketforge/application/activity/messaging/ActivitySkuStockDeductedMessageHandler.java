package com.lavyoung.marketforge.application.activity.messaging;

import com.lavyoung.marketforge.domain.activity.event.ActivitySkuStockDeductedEvent;
import com.lavyoung.marketforge.types.messaging.MessageContext;
import com.lavyoung.marketforge.types.messaging.MessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * sku库存扣减事件的消费处理器：把一条 MQ 消息翻译成一次用例调用。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivitySkuStockDeductedMessageHandler implements MessageHandler<ActivitySkuStockDeductedEvent> {

    @Override
    public void handle(ActivitySkuStockDeductedEvent event, MessageContext context) {
        log.info("活动sku库存扣减事件：sku={}, activityId={} occurredAt={}", event.sku(), event.activityId(), event.occurredAt());
    }
}


