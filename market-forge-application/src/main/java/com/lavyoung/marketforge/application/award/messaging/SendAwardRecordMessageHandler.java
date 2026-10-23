package com.lavyoung.marketforge.application.award.messaging;

import com.lavyoung.marketforge.application.award.model.GrantAwardCommand;
import com.lavyoung.marketforge.application.award.service.IAwardApplicationService;
import com.lavyoung.marketforge.domain.award.event.AwardFulfillmentSnapshot;
import com.lavyoung.marketforge.domain.award.event.SendAwardRecordEvent;
import com.lavyoung.marketforge.types.messaging.MessageContext;
import com.lavyoung.marketforge.types.messaging.MessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户中奖记录创建事件处理器。
 *
 * <p>该处理器只承担消息协议到发奖应用命令的适配职责，不解析奖品配置，
 * 也不决定具体发奖通道。奖品处理器选择和幂等履约由发奖应用服务完成。</p>
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
     * 将可靠消息转换为发奖应用命令。
     *
     * <p>日志只记录奖品类型和配置版本，不记录完整配置内容，避免券码模板、
     * 供应商参数或后续敏感履约信息进入日志系统。</p>
     *
     * @param event   用户中奖记录创建事件
     * @param context 消息上下文
     */
    @Override
    public void handle(SendAwardRecordEvent event, MessageContext context) {
        AwardFulfillmentSnapshot snapshot = event.fulfillmentSnapshot();

        log.info(
                "收到用户中奖记录事件 messageId={} userId={} orderId={} awardId={} awardCode={} awardType={} configVersion={}",
                context.messageId(),
                event.userId(),
                event.orderId(),
                event.awardId(),
                snapshot.awardCode(),
                snapshot.awardType(),
                snapshot.configVersion()
        );

        awardApplicationService.grantAward(new GrantAwardCommand(
                event.userId(),
                event.orderId(),
                event.awardId(),
                snapshot,
                event.awardTitle()
        ));
    }
}
