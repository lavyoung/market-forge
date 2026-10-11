package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.credit.model.aggregate.CreditAccountAdjustAggregate;
import com.lavyoung.marketforge.domain.credit.model.entity.CreditAccountOrderEntity;
import com.lavyoung.marketforge.domain.credit.repository.ICreditAccountRepository;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.credit.CreditAccountOrderAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.credit.IUserCreditAccountDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.credit.IUserCreditAccountOrderDao;
import com.lavyoung.marketforge.infrastructure.persistent.po.credit.UserCreditAccountOrderPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.credit.UserCreditAccountPO;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;


/**
 * 用户积分账户仓储实现。
 * <p>
 * 负责将积分账户订单和账户余额变动落到数据库。该实现使用积分订单的
 * {@code bizId} 唯一索引做幂等控制，避免 MQ 重复消费导致重复加积分。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
@Repository
@RequiredArgsConstructor
public class CreditAccountRepository implements ICreditAccountRepository {

    private final IUserCreditAccountDao userCreditAccountDao;
    private final IUserCreditAccountOrderDao userCreditAccountOrderDao;

    private final CreditAccountOrderAssembler creditAccountOrderAssembler;

    @Override
    public void saveCreditAccountAdjust(CreditAccountAdjustAggregate aggregate) {
        CreditAccountOrderEntity order = aggregate.order();

        saveOrderIdempotently(order);
        increaseOrCreateAccount(order.userId(), order.amount());
    }

    private void saveOrderIdempotently(CreditAccountOrderEntity order) {
        UserCreditAccountOrderPO orderPO = creditAccountOrderAssembler.toPO(order);
        try {
            int rows = userCreditAccountOrderDao.insert(orderPO);
            if (rows < 1) {
                throw BusinessException.of(BusinessResponseCode.POINTS_GRANT_FAILED, order.userId(), order.bizId());
            }
        } catch (DuplicateKeyException e) {
            throw BusinessException.of(BusinessResponseCode.POINTS_TRANSACTION_DUPLICATED, order.userId(), order.bizId());
        }
    }

    private void increaseOrCreateAccount(String userId, Long amount) {
        int rows = userCreditAccountDao.incrementCreditAmount(userId, amount);
        if (rows == 1) {
            return;
        }
        UserCreditAccountPO accountPO = new UserCreditAccountPO();
        accountPO.setUserId(userId);
        accountPO.setTotalAmount(amount);
        accountPO.setAvailableAmount(amount);

        try {
            int inserted = userCreditAccountDao.insert(accountPO);
            if (inserted < 1) {
                throw BusinessException.of(BusinessResponseCode.POINTS_GRANT_FAILED, userId, amount);
            }
        } catch (DuplicateKeyException e) {
            int retryUpdated = userCreditAccountDao.incrementCreditAmount(userId, amount);
            if (retryUpdated != 1) {
                throw BusinessException.of(BusinessResponseCode.POINTS_GRANT_FAILED, userId, amount);
            }
        }
    }
}
