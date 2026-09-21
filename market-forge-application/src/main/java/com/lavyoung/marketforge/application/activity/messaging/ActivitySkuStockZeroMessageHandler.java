package com.lavyoung.marketforge.application.activity.messaging;

import com.lavyoung.marketforge.domain.activity.event.ActivitySkuZeroStockEvent;
import com.lavyoung.marketforge.types.messaging.MessageContext;
import com.lavyoung.marketforge.types.messaging.MessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivitySkuStockZeroMessageHandler implements MessageHandler<ActivitySkuZeroStockEvent> {

    @Override
    public void handle(ActivitySkuZeroStockEvent event, MessageContext context) {
        log.info("活动sku库存为0事件：sku={}, occurredAt={}", event.sku(), event.occurredAt());

        // 监听 MQ 消息，处理库存为0，以及清空延迟队列
    }
}


