package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.credit.model.aggregate.CreditAccountAdjustAggregate;
import com.lavyoung.marketforge.domain.credit.model.entity.CreditAccountOrderEntity;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountBusinessTypeVO;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountOrderStateVO;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountTradeTypeVO;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.credit.CreditAccountOrderAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.credit.IUserCreditAccountDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.credit.IUserCreditAccountOrderDao;
import com.lavyoung.marketforge.infrastructure.persistent.po.credit.UserCreditAccountOrderPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.credit.UserCreditAccountPO;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 用户积分账户仓储测试。
 * <p>
 * 覆盖积分账户订单幂等、账户余额累加和并发创建兜底路径，防止重复消息导致重复加积分。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CreditAccountRepositoryTest {

    private static final String USER_ID = "user-001";
    private static final String BIZ_ID = "raffle-order-001";
    private static final Long AMOUNT = 10L;

    @Mock
    private IUserCreditAccountDao userCreditAccountDao;

    @Mock
    private IUserCreditAccountOrderDao userCreditAccountOrderDao;

    @Mock
    private CreditAccountOrderAssembler creditAccountOrderAssembler;

    /**
     * Given 积分账户已存在，When 保存积分入账聚合，Then 应插入账单并原子累加账户余额。
     */
    @Test
    void shouldInsertOrderAndIncrementExistingAccount() {
        // Given
        CreditAccountRepository repository = repository();
        CreditAccountOrderEntity order = order();
        UserCreditAccountOrderPO orderPO = new UserCreditAccountOrderPO();
        when(creditAccountOrderAssembler.toPO(order)).thenReturn(orderPO);
        when(userCreditAccountOrderDao.insert(orderPO)).thenReturn(1);
        when(userCreditAccountDao.incrementCreditAmount(USER_ID, AMOUNT)).thenReturn(1);

        // When
        repository.saveCreditAccountAdjust(new CreditAccountAdjustAggregate(order));

        // Then
        verify(userCreditAccountOrderDao).insert(orderPO);
        verify(userCreditAccountDao).incrementCreditAmount(USER_ID, AMOUNT);
        verify(userCreditAccountDao, never()).insert(any(UserCreditAccountPO.class));
    }

    /**
     * Given 积分账户不存在，When 保存积分入账聚合，Then 应创建账户并设置初始累计和可用余额。
     */
    @Test
    void shouldCreateAccountWhenAccountDoesNotExist() {
        // Given
        CreditAccountRepository repository = repository();
        CreditAccountOrderEntity order = order();
        UserCreditAccountOrderPO orderPO = new UserCreditAccountOrderPO();
        when(creditAccountOrderAssembler.toPO(order)).thenReturn(orderPO);
        when(userCreditAccountOrderDao.insert(orderPO)).thenReturn(1);
        when(userCreditAccountDao.incrementCreditAmount(USER_ID, AMOUNT)).thenReturn(0);
        when(userCreditAccountDao.insert(any(UserCreditAccountPO.class))).thenReturn(1);

        // When
        repository.saveCreditAccountAdjust(new CreditAccountAdjustAggregate(order));

        // Then
        ArgumentCaptor<UserCreditAccountPO> captor = ArgumentCaptor.forClass(UserCreditAccountPO.class);
        verify(userCreditAccountDao).insert(captor.capture());
        UserCreditAccountPO accountPO = captor.getValue();
        assertAll(
                () -> assertEquals(USER_ID, accountPO.getUserId()),
                () -> assertEquals(AMOUNT, accountPO.getTotalAmount()),
                () -> assertEquals(AMOUNT, accountPO.getAvailableAmount())
        );
    }

    /**
     * Given 两个线程并发创建同一账户，When 当前线程插入账户遇到唯一键冲突，Then 应重试累加余额。
     */
    @Test
    void shouldRetryIncrementWhenConcurrentAccountCreateHappens() {
        // Given
        CreditAccountRepository repository = repository();
        CreditAccountOrderEntity order = order();
        UserCreditAccountOrderPO orderPO = new UserCreditAccountOrderPO();
        when(creditAccountOrderAssembler.toPO(order)).thenReturn(orderPO);
        when(userCreditAccountOrderDao.insert(orderPO)).thenReturn(1);
        when(userCreditAccountDao.incrementCreditAmount(USER_ID, AMOUNT))
                .thenReturn(0)
                .thenReturn(1);
        when(userCreditAccountDao.insert(any(UserCreditAccountPO.class)))
                .thenThrow(new DuplicateKeyException("duplicate account"));

        // When
        repository.saveCreditAccountAdjust(new CreditAccountAdjustAggregate(order));

        // Then
        verify(userCreditAccountDao).insert(any(UserCreditAccountPO.class));
        verify(userCreditAccountDao, org.mockito.Mockito.times(2)).incrementCreditAmount(USER_ID, AMOUNT);
    }

    /**
     * Given 积分订单业务幂等号已存在，When 保存积分入账聚合，Then 应抛重复流水异常且不更新账户余额。
     */
    @Test
    void shouldRejectDuplicatedCreditOrderWithoutUpdatingAccount() {
        // Given
        CreditAccountRepository repository = repository();
        CreditAccountOrderEntity order = order();
        UserCreditAccountOrderPO orderPO = new UserCreditAccountOrderPO();
        when(creditAccountOrderAssembler.toPO(order)).thenReturn(orderPO);
        when(userCreditAccountOrderDao.insert(orderPO))
                .thenThrow(new DuplicateKeyException("duplicated biz id"));

        // When
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> repository.saveCreditAccountAdjust(new CreditAccountAdjustAggregate(order))
        );

        // Then
        assertEquals(BusinessResponseCode.POINTS_TRANSACTION_DUPLICATED, exception.getResponseCode());
        verify(userCreditAccountDao, never()).incrementCreditAmount(USER_ID, AMOUNT);
        verify(userCreditAccountDao, never()).insert(any(UserCreditAccountPO.class));
    }

    /**
     * Given 积分账户余额充足，When 保存积分支出聚合，Then 应插入账单并原子扣减账户可用余额。
     */
    @Test
    void shouldInsertOrderAndDecrementAccountWhenExpenseHappens() {
        // Given
        CreditAccountRepository repository = repository();
        CreditAccountOrderEntity order = expenseOrder();
        UserCreditAccountOrderPO orderPO = new UserCreditAccountOrderPO();
        when(creditAccountOrderAssembler.toPO(order)).thenReturn(orderPO);
        when(userCreditAccountOrderDao.insert(orderPO)).thenReturn(1);
        when(userCreditAccountDao.decrementCreditAmount(USER_ID, AMOUNT)).thenReturn(1);

        // When
        repository.saveCreditAccountAdjust(new CreditAccountAdjustAggregate(order));

        // Then
        verify(userCreditAccountOrderDao).insert(orderPO);
        verify(userCreditAccountDao).decrementCreditAmount(USER_ID, AMOUNT);
        verify(userCreditAccountDao, never()).incrementCreditAmount(USER_ID, AMOUNT);
        verify(userCreditAccountDao, never()).insert(any(UserCreditAccountPO.class));
    }

    /**
     * Given 积分账户不存在或余额不足，When 保存积分支出聚合，Then 应抛余额不足异常且不创建账户。
     */
    @Test
    void shouldRejectExpenseWhenBalanceIsNotEnough() {
        // Given
        CreditAccountRepository repository = repository();
        CreditAccountOrderEntity order = expenseOrder();
        UserCreditAccountOrderPO orderPO = new UserCreditAccountOrderPO();
        when(creditAccountOrderAssembler.toPO(order)).thenReturn(orderPO);
        when(userCreditAccountOrderDao.insert(orderPO)).thenReturn(1);
        when(userCreditAccountDao.decrementCreditAmount(USER_ID, AMOUNT)).thenReturn(0);

        // When
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> repository.saveCreditAccountAdjust(new CreditAccountAdjustAggregate(order))
        );

        // Then
        assertEquals(BusinessResponseCode.POINTS_BALANCE_NOT_ENOUGH, exception.getResponseCode());
        verify(userCreditAccountDao).decrementCreditAmount(USER_ID, AMOUNT);
        verify(userCreditAccountDao, never()).insert(any(UserCreditAccountPO.class));
    }

    private CreditAccountRepository repository() {
        return new CreditAccountRepository(
                userCreditAccountDao,
                userCreditAccountOrderDao,
                creditAccountOrderAssembler
        );
    }

    private CreditAccountOrderEntity order() {
        return new CreditAccountOrderEntity(
                USER_ID,
                "credit-order-001",
                BIZ_ID,
                CreditAccountTradeTypeVO.INCOME,
                CreditAccountBusinessTypeVO.AWARD_GRANT,
                AMOUNT,
                CreditAccountOrderStateVO.COMPLETED,
                LocalDateTime.of(2026, 10, 11, 20, 0)
        );
    }

    private CreditAccountOrderEntity expenseOrder() {
        return new CreditAccountOrderEntity(
                USER_ID,
                "credit-order-002",
                "exchange-order-001",
                CreditAccountTradeTypeVO.EXPENSE,
                CreditAccountBusinessTypeVO.EXCHANGE,
                AMOUNT,
                CreditAccountOrderStateVO.COMPLETED,
                LocalDateTime.of(2026, 10, 11, 20, 30)
        );
    }
}
