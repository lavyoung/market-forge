package com.lavyoung.marketforge.application.award.service;

import com.lavyoung.marketforge.application.award.model.GrantAwardCommand;
import com.lavyoung.marketforge.application.award.service.impl.AwardApplicationServiceImpl;
import com.lavyoung.marketforge.domain.award.repository.IAwardRepository;
import com.lavyoung.marketforge.domain.award.service.IAwardStateService;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountBusinessTypeVO;
import com.lavyoung.marketforge.domain.credit.service.ICreditAccountService;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 奖品应用服务测试。
 * <p>
 * 覆盖自动发奖编排的关键路径：积分奖品应完成积分入账和中奖记录状态推进，
 * 非积分奖品或非法积分配置应阻断发奖流程。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AwardApplicationServiceImplTest {

    private static final String USER_ID = "user-001";
    private static final String ORDER_ID = "order-001";
    private static final Long AWARD_ID = 100011L;

    @Mock
    private IAwardRepository awardRepository;

    @Mock
    private IAwardStateService awardStateService;

    @Mock
    private ICreditAccountService creditAccountService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    /**
     * Given 积分奖品发奖命令，When 发放奖品，Then 应先发放积分再完成中奖记录状态。
     */
    @Test
    void shouldGrantIntegralAwardAndCompleteAwardRecord() {
        // Given
        AwardApplicationServiceImpl service = service();
        GrantAwardCommand command = integralAwardCommand("10");

        // When
        service.grantAward(command);

        // Then
        verify(creditAccountService).grantCredit(
                USER_ID,
                ORDER_ID,
                10L,
                CreditAccountBusinessTypeVO.AWARD_GRANT
        );
        verify(awardStateService).completeGrant(USER_ID, ORDER_ID);
    }

    /**
     * Given 非积分奖品发奖命令，When 发放奖品，Then 应抛出类型不支持异常且不执行积分入账。
     */
    @Test
    void shouldRejectUnsupportedAwardType() {
        // Given
        AwardApplicationServiceImpl service = service();
        GrantAwardCommand command = new GrantAwardCommand(
                USER_ID,
                ORDER_ID,
                AWARD_ID,
                "coupon",
                "coupon-template-001",
                "测试优惠券"
        );

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.grantAward(command));

        // Then
        assertEquals(BusinessResponseCode.AWARD_TYPE_NOT_SUPPORTED, exception.getResponseCode());
        verify(creditAccountService, never()).grantCredit(
                USER_ID,
                ORDER_ID,
                10L,
                CreditAccountBusinessTypeVO.AWARD_GRANT
        );
        verify(awardStateService, never()).completeGrant(USER_ID, ORDER_ID);
    }

    /**
     * Given 积分数量不是数字，When 发放奖品，Then 应抛出积分数量非法异常。
     */
    @Test
    void shouldRejectInvalidIntegralAwardConfig() {
        // Given
        AwardApplicationServiceImpl service = service();
        GrantAwardCommand command = integralAwardCommand("abc");

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.grantAward(command));

        // Then
        assertEquals(BusinessResponseCode.USER_AWARD_POINTS_AMOUNT_INVALID, exception.getResponseCode());
        verify(creditAccountService, never()).grantCredit(
                USER_ID,
                ORDER_ID,
                10L,
                CreditAccountBusinessTypeVO.AWARD_GRANT
        );
        verify(awardStateService, never()).completeGrant(USER_ID, ORDER_ID);
    }

    /**
     * Given 积分数量小于等于零，When 发放奖品，Then 应抛出积分数量非法异常。
     */
    @Test
    void shouldRejectNonPositiveIntegralAwardConfig() {
        // Given
        AwardApplicationServiceImpl service = service();
        GrantAwardCommand command = integralAwardCommand("0");

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.grantAward(command));

        // Then
        assertEquals(BusinessResponseCode.USER_AWARD_POINTS_AMOUNT_INVALID, exception.getResponseCode());
        verify(creditAccountService, never()).grantCredit(
                USER_ID,
                ORDER_ID,
                10L,
                CreditAccountBusinessTypeVO.AWARD_GRANT
        );
        verify(awardStateService, never()).completeGrant(USER_ID, ORDER_ID);
    }

    /**
     * Given 积分账户流水已处理，When 重复发放同一中奖积分，Then 应视为幂等成功并推进中奖记录完成。
     */
    @Test
    void shouldTreatDuplicatedCreditTransactionAsIdempotentSuccess() {
        // Given
        AwardApplicationServiceImpl service = service();
        GrantAwardCommand command = integralAwardCommand("10");
        doThrow(BusinessException.of(BusinessResponseCode.POINTS_TRANSACTION_DUPLICATED, USER_ID, ORDER_ID))
                .when(creditAccountService)
                .grantCredit(USER_ID, ORDER_ID, 10L, CreditAccountBusinessTypeVO.AWARD_GRANT);

        // When / Then
        assertDoesNotThrow(() -> service.grantAward(command));
        verify(awardStateService).completeGrant(USER_ID, ORDER_ID);
    }

    /**
     * Given 积分账户真实发放失败，When 发放中奖积分，Then 应继续抛出异常并阻断中奖记录完成。
     */
    @Test
    void shouldPropagateCreditGrantFailure() {
        // Given
        AwardApplicationServiceImpl service = service();
        GrantAwardCommand command = integralAwardCommand("10");
        doThrow(BusinessException.of(BusinessResponseCode.POINTS_GRANT_FAILED, USER_ID, ORDER_ID))
                .when(creditAccountService)
                .grantCredit(USER_ID, ORDER_ID, 10L, CreditAccountBusinessTypeVO.AWARD_GRANT);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.grantAward(command));

        // Then
        assertEquals(BusinessResponseCode.POINTS_GRANT_FAILED, exception.getResponseCode());
        verify(awardStateService, never()).completeGrant(USER_ID, ORDER_ID);
    }

    private AwardApplicationServiceImpl service() {
        return new AwardApplicationServiceImpl(
                awardRepository,
                awardStateService,
                creditAccountService,
                eventPublisher
        );
    }

    private GrantAwardCommand integralAwardCommand(String awardConfig) {
        return new GrantAwardCommand(
                USER_ID,
                ORDER_ID,
                AWARD_ID,
                "integral",
                awardConfig,
                "测试积分"
        );
    }
}
