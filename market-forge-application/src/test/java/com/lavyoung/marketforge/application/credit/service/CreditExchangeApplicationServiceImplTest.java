package com.lavyoung.marketforge.application.credit.service;

import com.lavyoung.marketforge.application.credit.model.ExchangeCreditForSkuCommand;
import com.lavyoung.marketforge.application.credit.service.impl.CreditExchangeApplicationServiceImpl;
import com.lavyoung.marketforge.domain.activity.model.entity.SkuRechargeEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.OrderTradeTypeVO;
import com.lavyoung.marketforge.domain.activity.service.IRaffleActivityAccountQuotaService;
import com.lavyoung.marketforge.domain.credit.model.entity.CreditAdjustEntity;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountBusinessTypeVO;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountTradeTypeVO;
import com.lavyoung.marketforge.domain.credit.service.ICreditAccountService;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 积分兑换应用服务测试。
 *
 * <p>该应用服务横跨积分账户和活动账户两个领域。测试重点验证编排顺序、幂等号透传、
 * 参数快速失败，以及前置扣积分失败时不会继续创建活动 SKU 充值订单。</p>
 */
class CreditExchangeApplicationServiceImplTest {

    private static final String USER_ID = "user-001";
    private static final Long SKU = 9011L;
    private static final Long CREDIT_AMOUNT = 100L;
    private static final String OUT_BUSINESS_NO = "exchange-20261011-user-001-9011";

    /**
     * Given 合法积分兑换命令，When 执行兑换，Then 应先扣积分再创建活动 SKU 充值订单。
     */
    @Test
    void shouldDeductCreditAndCreateSkuRechargeOrderWhenExchangeSucceeds() {
        // Given
        ICreditAccountService creditAccountService = mock(ICreditAccountService.class);
        IRaffleActivityAccountQuotaService quotaService = mock(IRaffleActivityAccountQuotaService.class);
        when(quotaService.createSkuRechargeOrder(any())).thenReturn("activity-order-001");
        CreditExchangeApplicationServiceImpl service = new CreditExchangeApplicationServiceImpl(creditAccountService, quotaService);

        // When
        String orderId = service.exchangeCreditForSku(command());

        // Then
        assertEquals("activity-order-001", orderId);
        ArgumentCaptor<CreditAdjustEntity> creditCaptor = ArgumentCaptor.forClass(CreditAdjustEntity.class);
        ArgumentCaptor<SkuRechargeEntity> rechargeCaptor = ArgumentCaptor.forClass(SkuRechargeEntity.class);
        verify(creditAccountService).adjustCredit(creditCaptor.capture());
        verify(quotaService).createSkuRechargeOrder(rechargeCaptor.capture());

        CreditAdjustEntity creditAdjust = creditCaptor.getValue();
        SkuRechargeEntity recharge = rechargeCaptor.getValue();
        assertAll(
                () -> assertEquals(USER_ID, creditAdjust.userId()),
                () -> assertEquals(OUT_BUSINESS_NO, creditAdjust.bizId()),
                () -> assertEquals(CREDIT_AMOUNT, creditAdjust.amount()),
                () -> assertEquals(CreditAccountTradeTypeVO.EXPENSE, creditAdjust.tradeType()),
                () -> assertEquals(CreditAccountBusinessTypeVO.EXCHANGE, creditAdjust.businessType()),
                () -> assertEquals(USER_ID, recharge.userId()),
                () -> assertEquals(SKU, recharge.sku()),
                () -> assertEquals(OUT_BUSINESS_NO, recharge.outBusinessNo()),
                () -> assertEquals(OrderTradeTypeVO.CREDIT_PAY, recharge.orderTradeType())
        );
    }

    /**
     * Given 积分余额不足，When 执行兑换，Then 应直接抛出积分异常且不创建活动 SKU 充值订单。
     */
    @Test
    void shouldNotCreateSkuRechargeOrderWhenCreditDeductionFails() {
        // Given
        ICreditAccountService creditAccountService = mock(ICreditAccountService.class);
        IRaffleActivityAccountQuotaService quotaService = mock(IRaffleActivityAccountQuotaService.class);
        doThrow(BusinessException.of(BusinessResponseCode.POINTS_BALANCE_NOT_ENOUGH, USER_ID, CREDIT_AMOUNT))
                .when(creditAccountService)
                .adjustCredit(any());
        CreditExchangeApplicationServiceImpl service = new CreditExchangeApplicationServiceImpl(creditAccountService, quotaService);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.exchangeCreditForSku(command()));

        // Then
        assertEquals(BusinessResponseCode.POINTS_BALANCE_NOT_ENOUGH, exception.getResponseCode());
        verify(creditAccountService).adjustCredit(any());
        verifyNoInteractions(quotaService);
    }

    /**
     * Given 活动 SKU 充值失败，When 执行兑换，Then 应向上抛出异常，让事务回滚积分扣减。
     */
    @Test
    void shouldPropagateExceptionWhenSkuRechargeFails() {
        // Given
        ICreditAccountService creditAccountService = mock(ICreditAccountService.class);
        IRaffleActivityAccountQuotaService quotaService = mock(IRaffleActivityAccountQuotaService.class);
        BusinessException rechargeException = BusinessException.of(BusinessResponseCode.ACTIVITY_ACCOUNT_QUOTA_RECHARGE_FAILED, USER_ID, SKU);
        when(quotaService.createSkuRechargeOrder(any())).thenThrow(rechargeException);
        CreditExchangeApplicationServiceImpl service = new CreditExchangeApplicationServiceImpl(creditAccountService, quotaService);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.exchangeCreditForSku(command()));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_ACCOUNT_QUOTA_RECHARGE_FAILED, exception.getResponseCode());
        verify(creditAccountService).adjustCredit(any());
        verify(quotaService).createSkuRechargeOrder(any());
    }

    /**
     * Given 兑换命令缺少关键参数，When 执行兑换，Then 应快速失败且不访问任何领域服务。
     */
    @Test
    void shouldRejectInvalidCommandBeforeCallingDomainServices() {
        // Given
        ICreditAccountService creditAccountService = mock(ICreditAccountService.class);
        IRaffleActivityAccountQuotaService quotaService = mock(IRaffleActivityAccountQuotaService.class);
        CreditExchangeApplicationServiceImpl service = new CreditExchangeApplicationServiceImpl(creditAccountService, quotaService);
        ExchangeCreditForSkuCommand invalidCommand = new ExchangeCreditForSkuCommand(
                USER_ID,
                null,
                CREDIT_AMOUNT,
                OUT_BUSINESS_NO
        );

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.exchangeCreditForSku(invalidCommand));

        // Then
        assertEquals(BusinessResponseCode.POINTS_TRANSACTION_AMOUNT_INVALID, exception.getResponseCode());
        verifyNoInteractions(creditAccountService, quotaService);
    }

    private ExchangeCreditForSkuCommand command() {
        return new ExchangeCreditForSkuCommand(
                USER_ID,
                SKU,
                CREDIT_AMOUNT,
                OUT_BUSINESS_NO
        );
    }
}
