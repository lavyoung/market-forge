package com.lavyoung.marketforge.application.strategy.messaging;

import com.lavyoung.marketforge.domain.strategy.event.AwardStockDeductedEvent;
import com.lavyoung.marketforge.domain.strategy.service.IRaffleStock;
import com.lavyoung.marketforge.types.messaging.MessageContext;
import com.lavyoung.marketforge.types.messaging.MessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 奖品库存扣减事件的消费处理器：把一条 MQ 消息翻译成一次用例调用。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AwardStockDeductedMessageHandler implements MessageHandler<AwardStockDeductedEvent> {

    private final IRaffleStock raffleStock;

    @Override
    public void handle(AwardStockDeductedEvent event, MessageContext context) {
        log.info("收到奖品库存扣减事件 messageId={} traceId={} strategyId={} awardId={} userId={}",
                context.messageId(), context.traceId(),
                event.strategyId(), event.awardId(), event.userId());
        // 第 5 课在这里接上真正的库存扣减用例（IAwardStockService / IRaffleStock）。
        raffleStock.updateStrategyAwardStock(event.strategyId(), event.awardId());
    }
}


