package com.lavyoung.marketforge.domain.credit.service.impl;

import com.lavyoung.marketforge.domain.credit.model.aggregate.CreditAccountAdjustAggregate;
import com.lavyoung.marketforge.domain.credit.model.entity.CreditAccountOrderEntity;
import com.lavyoung.marketforge.domain.credit.model.entity.CreditAdjustEntity;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountBusinessTypeVO;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountOrderStateVO;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountTradeTypeVO;
import com.lavyoung.marketforge.domain.credit.repository.ICreditAccountRepository;
import com.lavyoung.marketforge.domain.credit.service.ICreditAccountService;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.utils.DateUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 用户积分账户领域服务默认实现。
 * <p>
 * 负责校验积分变动请求，并把一次积分入账转换为积分账户订单聚合。
 * 当前先实现中奖发奖产生的积分收入，后续积分兑换可以复用同一套账户订单模型。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditAccountServiceImpl implements ICreditAccountService {

    private final ICreditAccountRepository creditAccountRepository;

    @Override
    public void adjustCredit(CreditAdjustEntity creditAdjust) {
        String userId = creditAdjust.userId();
        String bizId = creditAdjust.bizId();
        Long amount = creditAdjust.amount();
        CreditAccountBusinessTypeVO businessType = creditAdjust.businessType();
        CreditAccountTradeTypeVO tradeType = creditAdjust.tradeType();
        log.info("积分入账：userId={} bizId={} amount={} businessType={} tradeType={}", userId, bizId, amount, businessType, tradeType);
        // 业务参数不合法
        if (StringUtils.isAnyBlank(userId, bizId) || amount == null || amount <= 0) {
            throw BusinessException.of(BusinessResponseCode.POINTS_TRANSACTION_AMOUNT_INVALID, userId, bizId, amount, tradeType, businessType);
        }

        // 业务参数不完整
        if (tradeType == null || businessType == null) {
            throw BusinessException.of(BusinessResponseCode.POINTS_TRANSACTION_STATE_ERROR, userId, bizId);
        }

        CreditAccountOrderEntity orderEntity = new CreditAccountOrderEntity(
                userId,
                UUID.randomUUID().toString().replace("-", ""),
                bizId,
                tradeType,
                businessType,
                amount,
                CreditAccountOrderStateVO.COMPLETED,
                DateUtil.now()
        );

        creditAccountRepository.saveCreditAccountAdjust(new CreditAccountAdjustAggregate(orderEntity));
    }
}
