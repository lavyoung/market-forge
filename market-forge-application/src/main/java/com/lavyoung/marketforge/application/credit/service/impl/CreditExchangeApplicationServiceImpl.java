package com.lavyoung.marketforge.application.credit.service.impl;

import com.lavyoung.marketforge.application.credit.model.ExchangeCreditForSkuCommand;
import com.lavyoung.marketforge.application.credit.service.ICreditExchangeApplicationService;
import com.lavyoung.marketforge.domain.activity.model.entity.SkuRechargeEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.OrderTradeTypeVO;
import com.lavyoung.marketforge.domain.activity.service.IRaffleActivityAccountQuotaService;
import com.lavyoung.marketforge.domain.credit.model.entity.CreditAdjustEntity;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountBusinessTypeVO;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountTradeTypeVO;
import com.lavyoung.marketforge.domain.credit.service.ICreditAccountService;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 积分兑换应用服务默认实现。
 *
 * <p>该实现负责完成“待支付活动 SKU 下单 + 积分支付”的跨领域编排。
 * 积分支付成功后，积分账户仓储会写入可靠消息任务；消息消费者收到积分调额成功事件后，
 * 再将待支付活动订单发货入账，给用户增加对应抽奖次数。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditExchangeApplicationServiceImpl implements ICreditExchangeApplicationService {

    private final ICreditAccountService creditAccountService;
    private final IRaffleActivityAccountQuotaService raffleActivityAccountQuotaService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String exchangeCreditForSku(ExchangeCreditForSkuCommand command) {
        ExchangeCreditForSkuCommand validateCommand = validateCommand(command);
        // 1. 先创建积分支付类型的活动 SKU 订单。
        // CREDIT_PAY 交易不会立即给用户增加抽奖次数，而是先写入 WAIT_PAY 待支付订单。
        String orderId = raffleActivityAccountQuotaService.createSkuRechargeOrder(new SkuRechargeEntity(
                validateCommand.userId(),
                validateCommand.sku(),
                validateCommand.outBusinessNo(),
                OrderTradeTypeVO.CREDIT_PAY
        ));

        // 2. 再扣减用户积分。
        // 扣减成功后，积分仓储会写入 CreditAdjustSuccessEvent 可靠消息任务，
        // 后续 MQ 消费者根据 userId + outBusinessNo 发货入账。
        creditAccountService.adjustCredit(new CreditAdjustEntity(
                validateCommand.userId(),
                validateCommand.outBusinessNo(),
                validateCommand.creditAmount(),
                CreditAccountTradeTypeVO.EXPENSE,
                CreditAccountBusinessTypeVO.EXCHANGE
        ));
        return orderId;
    }

    private ExchangeCreditForSkuCommand validateCommand(ExchangeCreditForSkuCommand command) {
        ExchangeCreditForSkuCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        if (StringUtils.isAnyBlank(validCommand.userId(), validCommand.outBusinessNo())
                || validCommand.sku() == null
                || validCommand.creditAmount() == null
                || validCommand.creditAmount() <= 0) {
            throw BusinessException.of(
                    BusinessResponseCode.POINTS_TRANSACTION_AMOUNT_INVALID,
                    validCommand.userId(),
                    validCommand.sku(),
                    validCommand.creditAmount(),
                    validCommand.outBusinessNo()
            );
        }
        return validCommand;
    }
}
