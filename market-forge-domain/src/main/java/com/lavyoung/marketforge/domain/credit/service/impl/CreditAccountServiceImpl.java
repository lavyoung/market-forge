package com.lavyoung.marketforge.domain.credit.service.impl;

import com.lavyoung.marketforge.domain.credit.model.aggregate.CreditAccountAdjustAggregate;
import com.lavyoung.marketforge.domain.credit.model.entity.CreditAccountOrderEntity;
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
    public void grantCredit(String userId, String bizId, Long amount, CreditAccountBusinessTypeVO businessType) {
        log.info("积分入账：userId={} bizId={} amount={}", userId, bizId, amount);
        if (StringUtils.isAnyBlank(userId, bizId) || amount == null || amount <= 0) {
            throw BusinessException.of(BusinessResponseCode.POINTS_TRANSACTION_AMOUNT_INVALID, userId, bizId, amount);
        }

        CreditAccountOrderEntity orderEntity = new CreditAccountOrderEntity(
                userId,
                UUID.randomUUID().toString().replace("-", ""),
                bizId,
                CreditAccountTradeTypeVO.INCOME,
                businessType,
                amount,
                CreditAccountOrderStateVO.COMPLETED,
                DateUtil.now()
        );

        creditAccountRepository.saveCreditAccountAdjust(new CreditAccountAdjustAggregate(orderEntity));
    }
}
