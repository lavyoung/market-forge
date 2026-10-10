package com.lavyoung.marketforge.application.behavior.service;

import com.lavyoung.marketforge.application.behavior.model.BehaviorRebateOrderCreateResult;
import com.lavyoung.marketforge.application.behavior.model.CreateBehaviorRebateOrderCommand;
import com.lavyoung.marketforge.application.behavior.service.impl.BehaviorRebateApplicationServiceImpl;
import com.lavyoung.marketforge.application.message.TaskCreatedEvent;
import com.lavyoung.marketforge.domain.behavior.model.entity.BehaviorEntity;
import com.lavyoung.marketforge.domain.behavior.model.entity.BehaviorRebateOrderResult;
import com.lavyoung.marketforge.domain.behavior.model.vo.BehaviorTypeVO;
import com.lavyoung.marketforge.domain.behavior.service.IBehaviorRebateService;
import com.lavyoung.marketforge.types.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证用户行为返利应用服务的用例编排和边界模型转换。
 */
class BehaviorRebateApplicationServiceImplTest {

    private static final String USER_ID = "user-001";
    private static final String OUT_BUSINESS_NO = "sign-20261010-user-001";
    private static final String ORDER_ID = "RO202610100001";
    private static final String EVENT_ID = "event-001";

    /**
     * Given 合法签到命令和领域创建结果，When 创建返利订单，Then 转换行为类型、返回应用结果并发布任务事件。
     */
    @Test
    void shouldCreateBehaviorRebateOrderAndPublishTaskEvent() {
        // Given
        IBehaviorRebateService behaviorRebateService = mock(IBehaviorRebateService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        when(behaviorRebateService.createOrder(any())).thenReturn(
                new BehaviorRebateOrderResult(List.of(ORDER_ID), List.of(EVENT_ID))
        );
        BehaviorRebateApplicationServiceImpl applicationService =
                new BehaviorRebateApplicationServiceImpl(behaviorRebateService, eventPublisher);

        // When
        BehaviorRebateOrderCreateResult result = applicationService.createOrder(
                new CreateBehaviorRebateOrderCommand(USER_ID, "sign", OUT_BUSINESS_NO)
        );

        // Then
        ArgumentCaptor<BehaviorEntity> behaviorCaptor = ArgumentCaptor.forClass(BehaviorEntity.class);
        verify(behaviorRebateService).createOrder(behaviorCaptor.capture());
        BehaviorEntity behavior = behaviorCaptor.getValue();
        assertAll(
                () -> assertEquals(USER_ID, behavior.userId()),
                () -> assertEquals(BehaviorTypeVO.SIGN, behavior.behaviorType()),
                () -> assertEquals(OUT_BUSINESS_NO, behavior.outBusinessNo()),
                () -> assertEquals(List.of(ORDER_ID), result.orderIds()),
                () -> assertEquals(List.of(EVENT_ID), result.eventIds())
        );
        verify(eventPublisher).publishEvent(new TaskCreatedEvent(USER_ID, EVENT_ID));
    }

    /**
     * Given 行为类型为空白，When 创建返利订单，Then 快速抛出业务异常且不进入领域服务。
     */
    @Test
    void shouldRejectBlankBehaviorType() {
        // Given
        IBehaviorRebateService behaviorRebateService = mock(IBehaviorRebateService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        BehaviorRebateApplicationServiceImpl applicationService =
                new BehaviorRebateApplicationServiceImpl(behaviorRebateService, eventPublisher);

        // When & Then
        assertThrows(BusinessException.class, () -> applicationService.createOrder(
                new CreateBehaviorRebateOrderCommand(USER_ID, " ", OUT_BUSINESS_NO)
        ));
        verifyNoInteractions(behaviorRebateService, eventPublisher);
    }

    /**
     * Given 未注册的行为类型编码，When 创建返利订单，Then 抛出业务异常且不发布任务事件。
     */
    @Test
    void shouldRejectUnknownBehaviorType() {
        // Given
        IBehaviorRebateService behaviorRebateService = mock(IBehaviorRebateService.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        BehaviorRebateApplicationServiceImpl applicationService =
                new BehaviorRebateApplicationServiceImpl(behaviorRebateService, eventPublisher);

        // When & Then
        assertThrows(BusinessException.class, () -> applicationService.createOrder(
                new CreateBehaviorRebateOrderCommand(USER_ID, "unknown", OUT_BUSINESS_NO)
        ));
        verifyNoInteractions(behaviorRebateService, eventPublisher);
    }
}
