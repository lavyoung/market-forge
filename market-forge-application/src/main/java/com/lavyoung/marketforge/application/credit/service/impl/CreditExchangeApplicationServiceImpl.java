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
 * <p>该实现负责完成“积分扣减 + 活动 SKU 充值”的跨领域编排。
 * 两个动作必须处于同一事务中：如果积分扣减成功但活动充值失败，本次兑换应整体回滚，
 * 避免用户被扣积分却没有获得抽奖次数。</p>
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
        // 扣积分
        creditAccountService.adjustCredit(new CreditAdjustEntity(
                validateCommand.userId(),
                validateCommand.outBusinessNo(),
                validateCommand.creditAmount(),
                CreditAccountTradeTypeVO.EXPENSE,
                CreditAccountBusinessTypeVO.EXCHANGE
        ));
        // 生成充值订单
        return raffleActivityAccountQuotaService.createSkuRechargeOrder(new SkuRechargeEntity(
                validateCommand.userId(),
                validateCommand.sku(),
                validateCommand.outBusinessNo(),
                OrderTradeTypeVO.CREDIT_PAY
        ));
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
