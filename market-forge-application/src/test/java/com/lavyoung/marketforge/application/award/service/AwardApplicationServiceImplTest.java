package com.lavyoung.marketforge.application.award.service;

import com.lavyoung.marketforge.application.award.grant.AwardGrantHandler;
import com.lavyoung.marketforge.application.award.grant.AwardGrantHandlerRegistry;
import com.lavyoung.marketforge.application.award.model.GrantAwardCommand;
import com.lavyoung.marketforge.application.award.service.impl.AwardApplicationServiceImpl;
import com.lavyoung.marketforge.domain.award.event.AwardFulfillmentSnapshot;
import com.lavyoung.marketforge.domain.award.repository.IAwardRepository;
import com.lavyoung.marketforge.domain.award.service.IAwardStateService;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * {@link AwardApplicationServiceImpl} 单元测试。
 *
 * <p>验证通用发奖应用服务只承担处理器路由和中奖记录状态编排。具体积分配置解析、
 * 积分入账和幂等语义由对应的发奖处理器测试覆盖。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AwardApplicationServiceImplTest {

    private static final String USER_ID = "user-001";
    private static final String ORDER_ID = "order-001";
    private static final Long AWARD_ID = 100011L;
    private static final String AWARD_TYPE = "internal_credit";

    @Mock
    private IAwardRepository awardRepository;

    @Mock
    private IAwardStateService awardStateService;

    @Mock
    private AwardGrantHandlerRegistry awardGrantHandlerRegistry;

    @Mock
    private AwardGrantHandler awardGrantHandler;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    /**
     * 验证应用服务按履约快照中的奖品类型选择处理器，并在履约成功后推进中奖记录。
     */
    @Test
    void shouldRouteHandlerAndCompleteAwardRecord() {
        // Given
        AwardApplicationServiceImpl service = service();
        GrantAwardCommand command = command();
        when(awardGrantHandlerRegistry.getRequired(AWARD_TYPE)).thenReturn(awardGrantHandler);

        // When
        service.grantAward(command);

        // Then
        InOrder inOrder = inOrder(awardGrantHandlerRegistry, awardGrantHandler, awardStateService);
        inOrder.verify(awardGrantHandlerRegistry).getRequired(AWARD_TYPE);
        inOrder.verify(awardGrantHandler).grant(command);
        inOrder.verify(awardStateService).completeGrant(USER_ID, ORDER_ID);
    }

    /**
     * 验证没有注册对应处理器时直接返回统一业务异常，不推进中奖记录状态。
     */
    @Test
    void shouldRejectUnregisteredAwardType() {
        // Given
        AwardApplicationServiceImpl service = service();
        GrantAwardCommand command = command();
        BusinessException unsupported = BusinessException.of(BusinessResponseCode.AWARD_GRANT_NOT_SUPPORTED, AWARD_TYPE);
        when(awardGrantHandlerRegistry.getRequired(AWARD_TYPE)).thenThrow(unsupported);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.grantAward(command));

        // Then
        assertEquals(BusinessResponseCode.AWARD_GRANT_NOT_SUPPORTED, exception.getResponseCode());
        verifyNoInteractions(awardGrantHandler);
        verify(awardStateService, never()).completeGrant(USER_ID, ORDER_ID);
    }

    /**
     * 验证处理器真实履约失败时异常继续向上抛出，中奖记录不能被错误标记为完成。
     */
    @Test
    void shouldNotCompleteAwardRecordWhenHandlerFails() {
        // Given
        AwardApplicationServiceImpl service = service();
        GrantAwardCommand command = command();
        BusinessException grantFailure = BusinessException.of(BusinessResponseCode.AWARD_GRANT_FAILED, ORDER_ID);
        when(awardGrantHandlerRegistry.getRequired(AWARD_TYPE)).thenReturn(awardGrantHandler);
        org.mockito.Mockito.doThrow(grantFailure).when(awardGrantHandler).grant(command);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> service.grantAward(command));

        // Then
        assertEquals(BusinessResponseCode.AWARD_GRANT_FAILED, exception.getResponseCode());
        verify(awardStateService, never()).completeGrant(USER_ID, ORDER_ID);
    }

    /**
     * 验证空命令在访问履约快照之前被明确拒绝。
     */
    @Test
    void shouldRejectNullGrantCommand() {
        // Given
        AwardApplicationServiceImpl service = service();

        // When
        NullPointerException exception = assertThrows(NullPointerException.class, () -> service.grantAward(null));

        // Then
        assertEquals("command must not be null", exception.getMessage());
        verifyNoInteractions(awardGrantHandlerRegistry, awardGrantHandler, awardStateService);
    }

    private AwardApplicationServiceImpl service() {
        return new AwardApplicationServiceImpl(
                awardRepository, awardStateService, awardGrantHandlerRegistry, eventPublisher);
    }

    private GrantAwardCommand command() {
        AwardFulfillmentSnapshot snapshot = new AwardFulfillmentSnapshot(
                "integral", AWARD_TYPE, "automatic", null, "{\"amount\":100}", 1);
        return new GrantAwardCommand(USER_ID, ORDER_ID, AWARD_ID, snapshot, "测试积分");
    }
}
