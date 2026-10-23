package com.lavyoung.marketforge.application.award.messaging;

import com.lavyoung.marketforge.application.award.model.GrantAwardCommand;
import com.lavyoung.marketforge.application.award.service.IAwardApplicationService;
import com.lavyoung.marketforge.domain.award.event.AwardFulfillmentSnapshot;
import com.lavyoung.marketforge.domain.award.event.SendAwardRecordEvent;
import com.lavyoung.marketforge.types.messaging.MessageContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;

/**
 * 用户中奖记录消息处理器测试。
 * <p>
 * 验证 MQ 处理器只承担消息适配职责，并将事件内容完整转换为发奖应用命令。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class SendAwardRecordMessageHandlerTest {

    @Mock
    private IAwardApplicationService awardApplicationService;

    /**
     * Given 用户中奖记录事件，When 消费消息，Then 应转成发奖命令交给应用服务。
     */
    @Test
    void shouldDispatchGrantAwardCommand() {
        // Given
        SendAwardRecordMessageHandler handler = new SendAwardRecordMessageHandler(awardApplicationService);
        AwardFulfillmentSnapshot snapshot = new AwardFulfillmentSnapshot(
                "credit_10", "internal_credit", "automatic", null, "{\"amount\":10}", 1);
        SendAwardRecordEvent event = new SendAwardRecordEvent(
                "user-001",
                "order-001",
                100011L,
                snapshot,
                "测试积分"
        );
        MessageContext context = new MessageContext("message-001", "trace-001", false, Map.of());

        // When
        handler.handle(event, context);

        // Then
        ArgumentCaptor<GrantAwardCommand> captor = ArgumentCaptor.forClass(GrantAwardCommand.class);
        verify(awardApplicationService).grantAward(captor.capture());

        GrantAwardCommand command = captor.getValue();
        assertEquals(event.userId(), command.userId());
        assertEquals(event.orderId(), command.orderId());
        assertEquals(event.awardId(), command.awardId());
        assertSame(snapshot, command.fulfillmentSnapshot());
        assertEquals(event.awardTitle(), command.awardTitle());
    }
}
