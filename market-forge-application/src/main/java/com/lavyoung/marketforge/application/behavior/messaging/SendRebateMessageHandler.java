package com.lavyoung.marketforge.application.behavior.messaging;

import com.lavyoung.marketforge.domain.activity.model.entity.SkuRechargeEntity;
import com.lavyoung.marketforge.domain.activity.service.IRaffleActivityAccountQuotaService;
import com.lavyoung.marketforge.domain.behavior.event.SendRebateEvent;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountBusinessTypeVO;
import com.lavyoung.marketforge.domain.credit.service.ICreditAccountService;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.messaging.MessageContext;
import com.lavyoung.marketforge.types.messaging.MessageHandler;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 用户行为返利消息处理器。
 *
 * <p>该处理器负责消费“用户行为返利发放事件”，并根据返利类型分发到对应的结算链路。
 * SKU 返利会进入活动额度账户，为用户增加活动抽奖次数；积分返利会进入用户积分账户，
 * 形成可追踪、可幂等的积分流水。</p>
 *
 * <p>消息处理器只负责应用层分发，不直接操作数据库。真正的账户变动由领域服务完成，
 * 从而保持 MQ 消费适配、应用编排和领域规则之间的职责边界清晰。</p>
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
    private final ICreditAccountService creditAccountService;

    /**
     * 处理用户行为返利发放事件。
     *
     * <p>消息处理规则：</p>
     * <ol>
     *     <li>先记录入站消息，方便排查 MQ 投递与消费链路。</li>
     *     <li>如果返利类型是 SKU，将返利配置转换为活动 SKU 编号并创建额度充值订单。</li>
     *     <li>如果返利类型是积分，将返利配置转换为积分数量并写入积分账户。</li>
     *     <li>如果返利类型暂未支持，则快速抛出参数异常，避免消息被静默吞掉。</li>
     * </ol>
     *
     * @param event   用户行为返利发放事件，包含用户、返利类型、返利配置和下游幂等号
     * @param context 消息上下文，包含消息编号、链路编号、是否重投等 MQ 元数据
     * @throws BusinessException 当返利配置不是合法数字，返利类型暂未支持，或账户入账失败时抛出
     */
    @Override
    public void handle(SendRebateEvent event, MessageContext context) {
        log.info("收到用户行为返利消息 messageId={} userId={} bizId={} rebateType={} rebateConfig={}",
                context.messageId(),
                event.userId(),
                event.bizId(),
                event.rebateType(),
                event.rebateConfig());
        switch (event.rebateType()) {
            case SKU -> handleSkuRebate(event, context);
            case INTEGRAL -> handleIntegralRebate(event, context);
            default -> throw BusinessException.of(CommonResponseCode.PARAM_INVALID, event.rebateType());
        }
    }

    /**
     * 将返利配置解析为活动 SKU 编号。
     *
     * <p>行为返利配置表里的 {@code rebate_config} 是字符串；在 SKU 返利场景下，
     * 它表示活动 SKU 编号，进入活动额度服务前需要转换成 {@link Long}。</p>
     */
    private void handleSkuRebate(SendRebateEvent event, MessageContext context) {
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

    /**
     * 将返利配置解析为积分数量并写入积分账户。
     *
     * <p>幂等号沿用行为返利订单的 {@code bizId}。积分账户流水表会对该字段建立唯一约束，
     * 从数据库层面保证 MQ 重复投递不会重复加分。</p>
     */
    private void handleIntegralRebate(SendRebateEvent event, MessageContext context) {
        Long amount;
        try {
            amount = Long.valueOf(event.rebateConfig());
        } catch (NumberFormatException e) {
            throw BusinessException.of(BusinessResponseCode.MESSAGE_FORMAT_INVALID, e, event.rebateConfig());
        }

        creditAccountService.grantCredit(event.userId(), event.bizId(), amount, CreditAccountBusinessTypeVO.SIGN_REBATE);
        log.info("用户行为积分返利结算完成 messageId={} userId={} bizId={} amount={}",
                context.messageId(), event.userId(), event.bizId(), amount);
    }
}
