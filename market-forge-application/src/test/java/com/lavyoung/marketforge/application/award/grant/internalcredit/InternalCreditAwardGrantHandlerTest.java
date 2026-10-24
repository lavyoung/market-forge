package com.lavyoung.marketforge.application.award.grant.internalcredit;

import com.lavyoung.marketforge.application.award.grant.AwardConfigDecoder;
import com.lavyoung.marketforge.application.award.model.GrantAwardCommand;
import com.lavyoung.marketforge.domain.award.event.AwardFulfillmentSnapshot;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountBusinessTypeVO;
import com.lavyoung.marketforge.domain.credit.service.ICreditAccountService;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * {@link InternalCreditAwardGrantHandler} 单元测试。
 *
 * <p>覆盖积分奖品履约的类型路由、配置版本、结构化配置解析、积分入账以及消息重试
 * 幂等语义。处理器成功只代表具体权益已经履约，不涉及中奖记录状态更新。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/23
 */
@ExtendWith(MockitoExtension.class)
class InternalCreditAwardGrantHandlerTest {

    private static final String USER_ID = "user-001";
    private static final String ORDER_ID = "order-001";
    private static final Long AWARD_ID = 100011L;
    private static final String CONFIG_CONTENT = "{\"amount\":100}";

    @Mock
    private AwardConfigDecoder configDecoder;

    @Mock
    private ICreditAccountService creditAccountService;

    /**
     * 验证处理器声明稳定的内部积分奖品类型，供注册表建立路由索引。
     */
    @Test
    void shouldDeclareInternalCreditAwardType() {
        // Given
        InternalCreditAwardGrantHandler handler = handler();

        // When / Then
        assertEquals("internal_credit", handler.awardType());
    }

    /**
     * 验证积分处理器只支持自动发放，不支持人工领取和空领取模式。
     */
    @Test
    void shouldOnlySupportAutomaticClaimMode() {
        // Given
        InternalCreditAwardGrantHandler handler = handler();

        // When / Then
        assertTrue(handler.supportsClaimMode("automatic"));
        assertFalse(handler.supportsClaimMode("manual"));
        assertFalse(handler.supportsClaimMode(null));
    }

    /**
     * 验证版本 1 的积分配置会先解码，再使用订单号完成积分入账。
     */
    @Test
    void shouldDecodeConfigAndGrantCredit() {
        // Given
        InternalCreditAwardGrantHandler handler = handler();
        GrantAwardCommand command = command("internal_credit", CONFIG_CONTENT, 1);
        InternalCreditAwardConfig config = new InternalCreditAwardConfig(100L);
        when(configDecoder.decode(CONFIG_CONTENT, InternalCreditAwardConfig.class)).thenReturn(config);

        // When
        handler.grant(command);

        // Then
        InOrder inOrder = inOrder(configDecoder, creditAccountService);
        inOrder.verify(configDecoder).decode(CONFIG_CONTENT, InternalCreditAwardConfig.class);
        inOrder.verify(creditAccountService).grantCredit(
                USER_ID, ORDER_ID, 100L, CreditAccountBusinessTypeVO.AWARD_GRANT);
    }

    /**
     * 验证重复积分流水代表先前已经入账，消息重试时应作为幂等成功返回。
     */
    @Test
    void shouldTreatDuplicatedCreditTransactionAsIdempotentSuccess() {
        // Given
        InternalCreditAwardGrantHandler handler = handler();
        GrantAwardCommand command = command("internal_credit", CONFIG_CONTENT, 1);
        when(configDecoder.decode(CONFIG_CONTENT, InternalCreditAwardConfig.class))
                .thenReturn(new InternalCreditAwardConfig(100L));
        doThrow(BusinessException.of(BusinessResponseCode.POINTS_TRANSACTION_DUPLICATED, USER_ID, ORDER_ID))
                .when(creditAccountService)
                .grantCredit(USER_ID, ORDER_ID, 100L, CreditAccountBusinessTypeVO.AWARD_GRANT);

        // When / Then
        assertDoesNotThrow(() -> handler.grant(command));
    }

    /**
     * 验证其他类型的命令不能误入积分处理器。
     */
    @Test
    void shouldRejectUnsupportedAwardType() {
        // Given
        InternalCreditAwardGrantHandler handler = handler();
        GrantAwardCommand command = command("third_party_coupon", CONFIG_CONTENT, 1);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> handler.grant(command));

        // Then
        assertEquals(BusinessResponseCode.AWARD_GRANT_NOT_SUPPORTED, exception.getResponseCode());
        verifyNoInteractions(configDecoder, creditAccountService);
    }

    /**
     * 验证未知配置版本不会套用版本 1 的解析规则。
     */
    @Test
    void shouldRejectUnsupportedConfigVersion() {
        // Given
        InternalCreditAwardGrantHandler handler = handler();
        GrantAwardCommand command = command("internal_credit", CONFIG_CONTENT, 2);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> handler.grant(command));

        // Then
        assertEquals(BusinessResponseCode.AWARD_CONFIG_INVALID, exception.getResponseCode());
        verifyNoInteractions(configDecoder, creditAccountService);
    }

    /**
     * 验证结构化配置解码失败时不会调用积分账户。
     */
    @Test
    void shouldPropagateConfigDecodeFailure() {
        // Given
        InternalCreditAwardGrantHandler handler = handler();
        GrantAwardCommand command = command("internal_credit", CONFIG_CONTENT, 1);
        BusinessException decodeFailure = BusinessException.of(BusinessResponseCode.AWARD_CONFIG_INVALID, AWARD_ID);
        when(configDecoder.decode(CONFIG_CONTENT, InternalCreditAwardConfig.class)).thenThrow(decodeFailure);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> handler.grant(command));

        // Then
        assertEquals(BusinessResponseCode.AWARD_CONFIG_INVALID, exception.getResponseCode());
        verify(creditAccountService, never()).grantCredit(
                USER_ID, ORDER_ID, 100L, CreditAccountBusinessTypeVO.AWARD_GRANT);
    }

    /**
     * 验证真实积分入账失败必须继续抛出，以便消息系统执行重试和死信策略。
     */
    @Test
    void shouldPropagateCreditGrantFailure() {
        // Given
        InternalCreditAwardGrantHandler handler = handler();
        GrantAwardCommand command = command("internal_credit", CONFIG_CONTENT, 1);
        when(configDecoder.decode(CONFIG_CONTENT, InternalCreditAwardConfig.class))
                .thenReturn(new InternalCreditAwardConfig(100L));
        doThrow(BusinessException.of(BusinessResponseCode.POINTS_GRANT_FAILED, USER_ID, ORDER_ID))
                .when(creditAccountService)
                .grantCredit(USER_ID, ORDER_ID, 100L, CreditAccountBusinessTypeVO.AWARD_GRANT);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> handler.grant(command));

        // Then
        assertEquals(BusinessResponseCode.POINTS_GRANT_FAILED, exception.getResponseCode());
    }

    /**
     * 验证空命令在读取履约快照前被明确拒绝。
     */
    @Test
    void shouldRejectNullCommand() {
        // Given
        InternalCreditAwardGrantHandler handler = handler();

        // When
        NullPointerException exception = assertThrows(NullPointerException.class, () -> handler.grant(null));

        // Then
        assertEquals("command must not be null", exception.getMessage());
        verifyNoInteractions(configDecoder, creditAccountService);
    }

    private InternalCreditAwardGrantHandler handler() {
        return new InternalCreditAwardGrantHandler(configDecoder, creditAccountService);
    }

    private GrantAwardCommand command(String awardType, String configContent, int configVersion) {
        AwardFulfillmentSnapshot snapshot = new AwardFulfillmentSnapshot(
                "integral", awardType, "automatic", null, configContent, configVersion);
        return new GrantAwardCommand(USER_ID, ORDER_ID, AWARD_ID, snapshot, "测试积分");
    }
}
