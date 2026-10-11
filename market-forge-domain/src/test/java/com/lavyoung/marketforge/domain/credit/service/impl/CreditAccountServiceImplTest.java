package com.lavyoung.marketforge.domain.credit.service.impl;

import com.lavyoung.marketforge.domain.credit.model.aggregate.CreditAccountAdjustAggregate;
import com.lavyoung.marketforge.domain.credit.model.entity.CreditAccountOrderEntity;
import com.lavyoung.marketforge.domain.credit.model.entity.CreditAdjustEntity;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountBusinessTypeVO;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountTradeTypeVO;
import com.lavyoung.marketforge.domain.credit.repository.ICreditAccountRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 用户积分账户领域服务测试。
 *
 * <p>领域服务负责把外部调额请求转换为积分账户订单聚合。这里重点验证收入和支出
 * 的交易方向不会丢失，并且非法调额参数会在进入仓储前被拦截。</p>
 */
class CreditAccountServiceImplTest {

    private static final String USER_ID = "user-001";
    private static final String BIZ_ID = "biz-001";

    /**
     * Given 发放积分请求，When 调用语义化快捷方法，Then 应创建收入方向的积分订单聚合。
     */
    @Test
    void shouldCreateIncomeOrderWhenGrantingCredit() {
        // Given
        ICreditAccountRepository repository = mock(ICreditAccountRepository.class);
        CreditAccountServiceImpl service = new CreditAccountServiceImpl(repository);

        // When
        service.grantCredit(USER_ID, BIZ_ID, 10L, CreditAccountBusinessTypeVO.AWARD_GRANT);

        // Then
        CreditAccountOrderEntity order = captureOrder(repository);
        assertAll(
                () -> assertEquals(USER_ID, order.userId()),
                () -> assertEquals(BIZ_ID, order.bizId()),
                () -> assertEquals(10L, order.amount()),
                () -> assertEquals(CreditAccountTradeTypeVO.INCOME, order.tradeType()),
                () -> assertEquals(CreditAccountBusinessTypeVO.AWARD_GRANT, order.businessType())
        );
    }

    /**
     * Given 积分兑换扣减请求，When 调用通用调额方法，Then 应创建支出方向的积分订单聚合。
     */
    @Test
    void shouldCreateExpenseOrderWhenAdjustingCreditWithExpenseTradeType() {
        // Given
        ICreditAccountRepository repository = mock(ICreditAccountRepository.class);
        CreditAccountServiceImpl service = new CreditAccountServiceImpl(repository);

        // When
        service.adjustCredit(new CreditAdjustEntity(
                USER_ID,
                BIZ_ID,
                100L,
                CreditAccountTradeTypeVO.EXPENSE,
                CreditAccountBusinessTypeVO.EXCHANGE
        ));

        // Then
        CreditAccountOrderEntity order = captureOrder(repository);
        assertAll(
                () -> assertEquals(USER_ID, order.userId()),
                () -> assertEquals(BIZ_ID, order.bizId()),
                () -> assertEquals(100L, order.amount()),
                () -> assertEquals(CreditAccountTradeTypeVO.EXPENSE, order.tradeType()),
                () -> assertEquals(CreditAccountBusinessTypeVO.EXCHANGE, order.businessType())
        );
    }

    /**
     * Given 调额请求缺少交易方向，When 调用通用调额方法，Then 应抛业务异常且不进入仓储。
     */
    @Test
    void shouldRejectMissingTradeTypeBeforeSavingAggregate() {
        // Given
        ICreditAccountRepository repository = mock(ICreditAccountRepository.class);
        CreditAccountServiceImpl service = new CreditAccountServiceImpl(repository);
        CreditAdjustEntity command = new CreditAdjustEntity(
                USER_ID,
                BIZ_ID,
                10L,
                null,
                CreditAccountBusinessTypeVO.SIGN_REBATE
        );

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.adjustCredit(command));

        // Then
        assertEquals(BusinessResponseCode.POINTS_TRANSACTION_STATE_ERROR, exception.getResponseCode());
        verifyNoInteractions(repository);
    }

    /**
     * Given 调额请求缺少业务类型，When 调用通用调额方法，Then 应抛业务异常且不进入仓储。
     */
    @Test
    void shouldRejectMissingBusinessTypeBeforeSavingAggregate() {
        // Given
        ICreditAccountRepository repository = mock(ICreditAccountRepository.class);
        CreditAccountServiceImpl service = new CreditAccountServiceImpl(repository);
        CreditAdjustEntity command = new CreditAdjustEntity(
                USER_ID,
                BIZ_ID,
                10L,
                CreditAccountTradeTypeVO.INCOME,
                null
        );

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.adjustCredit(command));

        // Then
        assertEquals(BusinessResponseCode.POINTS_TRANSACTION_STATE_ERROR, exception.getResponseCode());
        verifyNoInteractions(repository);
    }

    /**
     * Given 调额数量小于等于零，When 调用通用调额方法，Then 应抛金额非法异常且不进入仓储。
     */
    @Test
    void shouldRejectNonPositiveAmountBeforeSavingAggregate() {
        // Given
        ICreditAccountRepository repository = mock(ICreditAccountRepository.class);
        CreditAccountServiceImpl service = new CreditAccountServiceImpl(repository);
        CreditAdjustEntity command = new CreditAdjustEntity(
                USER_ID,
                BIZ_ID,
                0L,
                CreditAccountTradeTypeVO.INCOME,
                CreditAccountBusinessTypeVO.SIGN_REBATE
        );

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.adjustCredit(command));

        // Then
        assertEquals(BusinessResponseCode.POINTS_TRANSACTION_AMOUNT_INVALID, exception.getResponseCode());
        verifyNoInteractions(repository);
    }

    private CreditAccountOrderEntity captureOrder(ICreditAccountRepository repository) {
        ArgumentCaptor<CreditAccountAdjustAggregate> captor = ArgumentCaptor.forClass(CreditAccountAdjustAggregate.class);
        verify(repository).saveCreditAccountAdjust(captor.capture());
        return captor.getValue().order();
    }
}
