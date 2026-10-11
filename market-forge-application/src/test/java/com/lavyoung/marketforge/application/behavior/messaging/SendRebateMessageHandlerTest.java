package com.lavyoung.marketforge.application.behavior.messaging;

import com.lavyoung.marketforge.domain.activity.model.entity.SkuRechargeEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.OrderTradeTypeVO;
import com.lavyoung.marketforge.domain.activity.service.IRaffleActivityAccountQuotaService;
import com.lavyoung.marketforge.domain.behavior.event.SendRebateEvent;
import com.lavyoung.marketforge.domain.behavior.model.vo.RebateTypeVO;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountBusinessTypeVO;
import com.lavyoung.marketforge.domain.credit.service.ICreditAccountService;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.messaging.MessageContext;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证用户行为返利消息处理器的结算分发规则。
 *
 * <p>消息处理器位于 application 层，它不直接操作数据库，而是根据返利类型把消息分发到
 * 对应的领域服务：SKU 返利进入活动额度账户，积分返利进入积分账户。</p>
 */
class SendRebateMessageHandlerTest {

    private static final String USER_ID = "user-001";
    private static final String BIZ_ID = "user-001_sku_sign-20261010";
    private static final String MESSAGE_ID = "message-001";
    private static final MessageContext CONTEXT = new MessageContext(MESSAGE_ID, "trace-001", false, Map.of());

    /**
     * Given SKU 返利消息，When 处理返利事件，Then 将返利配置转换为活动 SKU 并创建活动额度充值订单。
     */
    @Test
    void shouldCreateSkuRechargeOrderWhenRebateTypeIsSku() {
        // Given
        IRaffleActivityAccountQuotaService quotaService = mock(IRaffleActivityAccountQuotaService.class);
        ICreditAccountService creditAccountService = mock(ICreditAccountService.class);
        when(quotaService.createSkuRechargeOrder(org.mockito.ArgumentMatchers.any()))
                .thenReturn("activity-order-001");
        SendRebateMessageHandler handler = new SendRebateMessageHandler(quotaService, creditAccountService);
        SendRebateEvent event = new SendRebateEvent(USER_ID, BIZ_ID, RebateTypeVO.SKU, "9011");

        // When
        handler.handle(event, CONTEXT);

        // Then
        ArgumentCaptor<SkuRechargeEntity> captor = ArgumentCaptor.forClass(SkuRechargeEntity.class);
        verify(quotaService).createSkuRechargeOrder(captor.capture());
        SkuRechargeEntity skuRecharge = captor.getValue();
        assertAll(
                () -> assertEquals(USER_ID, skuRecharge.userId()),
                () -> assertEquals(9011L, skuRecharge.sku()),
                () -> assertEquals(BIZ_ID, skuRecharge.outBusinessNo()),
                () -> assertEquals(OrderTradeTypeVO.REBATE_NO_PAY, skuRecharge.orderTradeType())
        );
        verifyNoInteractions(creditAccountService);
    }

    /**
     * Given 积分返利消息，When 处理返利事件，Then 将返利配置转换为积分数量并写入积分账户。
     */
    @Test
    void shouldGrantCreditWhenRebateTypeIsIntegral() {
        // Given
        IRaffleActivityAccountQuotaService quotaService = mock(IRaffleActivityAccountQuotaService.class);
        ICreditAccountService creditAccountService = mock(ICreditAccountService.class);
        SendRebateMessageHandler handler = new SendRebateMessageHandler(quotaService, creditAccountService);
        SendRebateEvent event = new SendRebateEvent(USER_ID, BIZ_ID, RebateTypeVO.INTEGRAL, "10");

        // When
        handler.handle(event, CONTEXT);

        // Then
        verifyNoInteractions(quotaService);
        verify(creditAccountService).grantCredit(USER_ID, BIZ_ID, 10L, CreditAccountBusinessTypeVO.SIGN_REBATE);
    }

    /**
     * Given 积分返利配置不是数字，When 处理返利事件，Then 抛出消息格式异常且不会写入积分账户。
     */
    @Test
    void shouldRejectInvalidIntegralConfig() {
        // Given
        IRaffleActivityAccountQuotaService quotaService = mock(IRaffleActivityAccountQuotaService.class);
        ICreditAccountService creditAccountService = mock(ICreditAccountService.class);
        SendRebateMessageHandler handler = new SendRebateMessageHandler(quotaService, creditAccountService);
        SendRebateEvent event = new SendRebateEvent(USER_ID, BIZ_ID, RebateTypeVO.INTEGRAL, "bad-credit");

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> handler.handle(event, CONTEXT));

        // Then
        assertEquals(BusinessResponseCode.MESSAGE_FORMAT_INVALID, exception.getResponseCode());
        verifyNoInteractions(quotaService);
        verifyNoInteractions(creditAccountService);
    }

    /**
     * Given SKU 返利配置不是数字，When 处理返利事件，Then 抛出消息格式异常且不会创建额度订单。
     */
    @Test
    void shouldRejectInvalidSkuConfig() {
        // Given
        IRaffleActivityAccountQuotaService quotaService = mock(IRaffleActivityAccountQuotaService.class);
        ICreditAccountService creditAccountService = mock(ICreditAccountService.class);
        SendRebateMessageHandler handler = new SendRebateMessageHandler(quotaService, creditAccountService);
        SendRebateEvent event = new SendRebateEvent(USER_ID, BIZ_ID, RebateTypeVO.SKU, "bad-sku");

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> handler.handle(event, CONTEXT));

        // Then
        assertEquals(BusinessResponseCode.MESSAGE_FORMAT_INVALID, exception.getResponseCode());
        verifyNoInteractions(quotaService);
        verifyNoInteractions(creditAccountService);
    }
}
