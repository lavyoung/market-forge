package com.lavyoung.marketforge.application.behavior.messaging;

import com.lavyoung.marketforge.domain.activity.model.entity.SkuRechargeEntity;
import com.lavyoung.marketforge.domain.activity.service.IRaffleActivityAccountQuotaService;
import com.lavyoung.marketforge.domain.behavior.event.SendRebateEvent;
import com.lavyoung.marketforge.domain.behavior.model.vo.RebateTypeVO;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.messaging.MessageContext;
import com.lavyoung.marketforge.types.messaging.MessageHandler;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户行为返利消息处理器。
 *
 * <p>该处理器负责消费“用户行为返利发放事件”，并根据返利类型分发到对应的结算链路。
 * 当前章节先实现 SKU 返利结算：当用户完成签到等行为后，系统根据返利配置找到活动 SKU，
 * 再调用活动账户额度服务，为用户增加活动抽奖次数。</p>
 *
 * <p>积分返利、优惠券返利等类型在本章暂不落地，只记录日志并跳过，避免把尚未实现的账户体系
 * 和当前章节目标混在一起。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SendRebateMessageHandler implements MessageHandler<SendRebateEvent> {

    private final IRaffleActivityAccountQuotaService raffleActivityAccountQuotaService;

    /**
     * 处理用户行为返利发放事件。
     *
     * <p>消息处理规则：</p>
     * <ol>
     *     <li>先记录入站消息，方便排查 MQ 投递与消费链路。</li>
     *     <li>如果返利类型不是 SKU，本章暂时跳过。</li>
     *     <li>如果返利类型是 SKU，将返利配置转换为活动 SKU 编号。</li>
     *     <li>调用活动账户额度服务创建 SKU 充值订单，完成抽奖次数入账。</li>
     * </ol>
     *
     * @param event   用户行为返利发放事件，包含用户、返利类型、返利配置和下游幂等号
     * @param context 消息上下文，包含消息编号、链路编号、是否重投等 MQ 元数据
     * @throws BusinessException 当 SKU 配置不是合法数字，或活动额度入账失败时抛出
     */
    @Override
    public void handle(SendRebateEvent event, MessageContext context) {
        log.info("收到用户行为返利消息 messageId={} userId={} bizId={} rebateType={} rebateConfig={}",
                context.messageId(),
                event.userId(),
                event.bizId(),
                event.rebateType(),
                event.rebateConfig());
        if (event.rebateType() != RebateTypeVO.SKU) {
            log.info("用户行为返利消息暂不处理非 SKU 返利 messageId={} userId={} bizId={} rebateType={}",
                    context.messageId(),
                    event.userId(),
                    event.bizId(),
                    event.rebateType());
            return;
        }
        /*
         * 将返利配置解析为活动 SKU 编号。
         *
         * <p>行为返利配置表里的 {@code rebate_config} 是字符串，因为不同返利类型的配置含义不同：
         * SKU 返利时它表示活动 SKU 编号，积分返利时它可以表示积分数量。进入 SKU 结算前，
         * 必须先把字符串配置转换成 {@link Long} 类型，才能调用活动账户额度服务。</p>
         */
        Long sku = null;
        try {
            sku = Long.valueOf(event.rebateConfig());
        } catch (NumberFormatException e) {
            throw BusinessException.of(BusinessResponseCode.MESSAGE_FORMAT_INVALID, e, event.rebateConfig());
        }
        String orderId = raffleActivityAccountQuotaService.createSkuRechargeOrder(new SkuRechargeEntity(event.userId(), sku, event.bizId()));
        log.info("用户行为 SKU 返利结算完成 messageId={} userId={} bizId={} sku={} orderId={}",
                context.messageId(),
                event.userId(),
                event.bizId(),
                sku,
                orderId);
    }
}
