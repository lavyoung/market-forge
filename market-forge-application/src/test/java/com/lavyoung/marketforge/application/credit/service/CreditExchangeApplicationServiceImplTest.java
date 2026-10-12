package com.lavyoung.marketforge.application.credit.service;

import com.lavyoung.marketforge.application.credit.model.ExchangeCreditForSkuCommand;
import com.lavyoung.marketforge.application.credit.service.impl.CreditExchangeApplicationServiceImpl;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.SkuRechargeEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.OrderTradeTypeVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.service.IRaffleActivityAccountQuotaService;
import com.lavyoung.marketforge.domain.credit.model.entity.CreditAdjustEntity;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountBusinessTypeVO;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountTradeTypeVO;
import com.lavyoung.marketforge.domain.credit.service.ICreditAccountService;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 积分兑换应用服务测试。
 *
 * <p>测试关注服务端定价的安全边界：应用服务必须按 SKU 查询运营配置的积分价格，
 * 不能接收或信任调用方自带的价格；只有价格有效时才允许创建待支付订单并扣减积分。</p>
 */
class CreditExchangeApplicationServiceImplTest {

    private static final String USER_ID = "user-001";
    private static final Long SKU = 9011L;
    private static final Long CREDIT_AMOUNT = 100L;
    private static final String OUT_BUSINESS_NO = "exchange-20261012-user-001-9011";

    /**
     * Given SKU 配置了有效积分价格，When 执行兑换，Then 应按服务端价格先创建订单再扣减积分。
     */
    @Test
    void shouldDeductCreditUsingServerConfiguredSkuPriceWhenExchangeSucceeds() {
        // Given
        ICreditAccountService creditAccountService = mock(ICreditAccountService.class);
        IRaffleActivityAccountQuotaService quotaService = mock(IRaffleActivityAccountQuotaService.class);
        IActivityRepository activityRepository = mock(IActivityRepository.class);
        when(activityRepository.queryActivitySku(SKU)).thenReturn(activitySku(CREDIT_AMOUNT));
        when(quotaService.createSkuRechargeOrder(any())).thenReturn("activity-order-001");
        CreditExchangeApplicationServiceImpl service = service(creditAccountService, quotaService, activityRepository);

        // When
        String orderId = service.exchangeCreditForSku(command());

        // Then
        ArgumentCaptor<CreditAdjustEntity> creditCaptor = ArgumentCaptor.forClass(CreditAdjustEntity.class);
        ArgumentCaptor<SkuRechargeEntity> rechargeCaptor = ArgumentCaptor.forClass(SkuRechargeEntity.class);
        InOrder inOrder = inOrder(activityRepository, quotaService, creditAccountService);
        inOrder.verify(activityRepository).queryActivitySku(SKU);
        inOrder.verify(quotaService).createSkuRechargeOrder(rechargeCaptor.capture());
        inOrder.verify(creditAccountService).adjustCredit(creditCaptor.capture());

        CreditAdjustEntity creditAdjust = creditCaptor.getValue();
        SkuRechargeEntity recharge = rechargeCaptor.getValue();
        assertAll(
                () -> assertEquals("activity-order-001", orderId),
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
     * Given SKU 不存在，When 执行兑换，Then 应快速失败且不得创建订单或扣减积分。
     */
    @Test
    void shouldRejectExchangeWhenSkuDoesNotExist() {
        // Given
        ICreditAccountService creditAccountService = mock(ICreditAccountService.class);
        IRaffleActivityAccountQuotaService quotaService = mock(IRaffleActivityAccountQuotaService.class);
        IActivityRepository activityRepository = mock(IActivityRepository.class);
        when(activityRepository.queryActivitySku(SKU)).thenReturn(null);
        CreditExchangeApplicationServiceImpl service = service(creditAccountService, quotaService, activityRepository);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.exchangeCreditForSku(command()));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_SKU_NOT_FOUND, exception.getResponseCode());
        verify(activityRepository).queryActivitySku(SKU);
        verifyNoInteractions(quotaService, creditAccountService);
    }

    /**
     * Given SKU 未配置积分价格，When 执行兑换，Then 应拒绝兑换且不得创建订单或扣减积分。
     */
    @Test
    void shouldRejectExchangeWhenSkuCreditAmountIsMissing() {
        // Given
        ICreditAccountService creditAccountService = mock(ICreditAccountService.class);
        IRaffleActivityAccountQuotaService quotaService = mock(IRaffleActivityAccountQuotaService.class);
        IActivityRepository activityRepository = mock(IActivityRepository.class);
        when(activityRepository.queryActivitySku(SKU)).thenReturn(activitySku(null));
        CreditExchangeApplicationServiceImpl service = service(creditAccountService, quotaService, activityRepository);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.exchangeCreditForSku(command()));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_SKU_CREDIT_AMOUNT_INVALID, exception.getResponseCode());
        verify(activityRepository).queryActivitySku(SKU);
        verifyNoInteractions(quotaService, creditAccountService);
    }

    /**
     * Given 待支付活动订单创建失败，When 执行兑换，Then 不得继续扣减积分。
     */
    @Test
    void shouldNotDeductCreditWhenSkuOrderCreationFails() {
        // Given
        ICreditAccountService creditAccountService = mock(ICreditAccountService.class);
        IRaffleActivityAccountQuotaService quotaService = mock(IRaffleActivityAccountQuotaService.class);
        IActivityRepository activityRepository = mock(IActivityRepository.class);
        BusinessException rechargeException = BusinessException.of(
                BusinessResponseCode.ACTIVITY_ACCOUNT_QUOTA_RECHARGE_FAILED,
                USER_ID,
                SKU
        );
        when(activityRepository.queryActivitySku(SKU)).thenReturn(activitySku(CREDIT_AMOUNT));
        when(quotaService.createSkuRechargeOrder(any())).thenThrow(rechargeException);
        CreditExchangeApplicationServiceImpl service = service(creditAccountService, quotaService, activityRepository);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.exchangeCreditForSku(command()));

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_ACCOUNT_QUOTA_RECHARGE_FAILED, exception.getResponseCode());
        verify(activityRepository).queryActivitySku(SKU);
        verify(quotaService).createSkuRechargeOrder(any());
        verifyNoInteractions(creditAccountService);
    }

    /**
     * Given 兑换命令缺少 SKU，When 执行兑换，Then 应在查询 SKU 前快速失败。
     */
    @Test
    void shouldRejectInvalidCommandBeforeCallingDomainServices() {
        // Given
        ICreditAccountService creditAccountService = mock(ICreditAccountService.class);
        IRaffleActivityAccountQuotaService quotaService = mock(IRaffleActivityAccountQuotaService.class);
        IActivityRepository activityRepository = mock(IActivityRepository.class);
        CreditExchangeApplicationServiceImpl service = service(creditAccountService, quotaService, activityRepository);
        ExchangeCreditForSkuCommand invalidCommand = new ExchangeCreditForSkuCommand(USER_ID, null, OUT_BUSINESS_NO);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.exchangeCreditForSku(invalidCommand));

        // Then
        assertEquals(BusinessResponseCode.POINTS_TRANSACTION_AMOUNT_INVALID, exception.getResponseCode());
        verifyNoInteractions(activityRepository, quotaService, creditAccountService);
    }

    /**
     * 创建默认积分兑换命令。
     *
     * @return 合法积分兑换命令
     */
    private ExchangeCreditForSkuCommand command() {
        return new ExchangeCreditForSkuCommand(USER_ID, SKU, OUT_BUSINESS_NO);
    }

    /**
     * 创建指定积分价格的活动 SKU 测试数据。
     *
     * @param creditAmount 服务端配置的兑换价格；可为空，用于验证配置缺失场景
     * @return 活动 SKU 领域实体
     */
    private ActivitySkuEntity activitySku(Long creditAmount) {
        return new ActivitySkuEntity(SKU, 100301L, 100301L, 100000, 100000, creditAmount);
    }

    /**
     * 创建被测积分兑换应用服务。
     *
     * @param creditAccountService 积分账户领域服务
     * @param quotaService         活动账户额度领域服务
     * @param activityRepository   活动配置仓储端口
     * @return 已注入测试替身的应用服务
     */
    private CreditExchangeApplicationServiceImpl service(
            ICreditAccountService creditAccountService,
            IRaffleActivityAccountQuotaService quotaService,
            IActivityRepository activityRepository
    ) {
        return new CreditExchangeApplicationServiceImpl(creditAccountService, quotaService, activityRepository);
    }
}
