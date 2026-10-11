package com.lavyoung.marketforge.domain.behavior.service.impl;

import com.lavyoung.marketforge.domain.behavior.event.SendRebateEvent;
import com.lavyoung.marketforge.domain.behavior.model.aggregate.UserBehaviorRebateAggregate;
import com.lavyoung.marketforge.domain.behavior.model.entity.BehaviorEntity;
import com.lavyoung.marketforge.domain.behavior.model.entity.BehaviorRebateConfigEntity;
import com.lavyoung.marketforge.domain.behavior.model.entity.BehaviorRebateOrderResult;
import com.lavyoung.marketforge.domain.behavior.model.entity.UserBehaviorRebateOrderEntity;
import com.lavyoung.marketforge.domain.behavior.model.vo.BehaviorTypeVO;
import com.lavyoung.marketforge.domain.behavior.model.vo.RebateTypeVO;
import com.lavyoung.marketforge.domain.behavior.repository.IBehaviorRebateRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证用户行为返利领域服务的订单与事件生成规则。
 *
 * <p>领域服务负责把一次用户行为转换为具体返利订单，并为每笔订单生成下游发放事件。
 * 本测试重点覆盖同一签到行为同时产生 SKU 和积分返利时，业务幂等号是否按返利类型隔离。</p>
 */
class BehaviorRebateServiceImplTest {

    private static final String USER_ID = "user-001";
    private static final String OUT_BUSINESS_NO = "sign-20261011-user-001";

    /**
     * Given 签到行为同时配置 SKU 和积分返利，When 创建返利订单，Then 生成两笔订单和两个发放事件。
     */
    @Test
    void shouldCreateOrdersAndEventsForMultipleRebateTypes() {
        // Given
        IBehaviorRebateRepository repository = mock(IBehaviorRebateRepository.class);
        BehaviorRebateServiceImpl service = new BehaviorRebateServiceImpl(repository);
        when(repository.queryBehaviorRebateConfig(BehaviorTypeVO.SIGN)).thenReturn(List.of(
                new BehaviorRebateConfigEntity(BehaviorTypeVO.SIGN, RebateTypeVO.SKU, "9011", "签到返利：发放活动 SKU"),
                new BehaviorRebateConfigEntity(BehaviorTypeVO.SIGN, RebateTypeVO.INTEGRAL, "10", "签到返利：发放 10 积分")
        ));
        when(repository.saveUserBehaviorRebateAggregate(any())).thenAnswer(invocation -> {
            UserBehaviorRebateAggregate aggregate = invocation.getArgument(0);
            return new BehaviorRebateOrderResult(
                    aggregate.orders().stream()
                            .map(UserBehaviorRebateOrderEntity::orderId)
                            .toList(),
                    aggregate.events().stream()
                            .map(SendRebateEvent::eventId)
                            .toList()
            );
        });

        // When
        BehaviorRebateOrderResult result = service.createOrder(new BehaviorEntity(
                USER_ID,
                BehaviorTypeVO.SIGN,
                OUT_BUSINESS_NO
        ));

        // Then
        ArgumentCaptor<UserBehaviorRebateAggregate> aggregateCaptor = ArgumentCaptor.forClass(UserBehaviorRebateAggregate.class);
        verify(repository).saveUserBehaviorRebateAggregate(aggregateCaptor.capture());
        UserBehaviorRebateAggregate aggregate = aggregateCaptor.getValue();
        assertAll(
                () -> assertEquals(USER_ID, aggregate.userId()),
                () -> assertEquals(2, aggregate.orders().size()),
                () -> assertEquals(2, aggregate.events().size()),
                () -> assertEquals(2, result.orderIds().size()),
                () -> assertEquals(2, result.eventIds().size())
        );

        UserBehaviorRebateOrderEntity skuOrder = aggregate.orders().get(0);
        UserBehaviorRebateOrderEntity integralOrder = aggregate.orders().get(1);
        assertAll(
                () -> assertEquals(RebateTypeVO.SKU, skuOrder.rebateType()),
                () -> assertEquals("9011", skuOrder.rebateConfig()),
                () -> assertEquals(USER_ID + "_sku_" + OUT_BUSINESS_NO, skuOrder.bizId()),
                () -> assertEquals(OUT_BUSINESS_NO, skuOrder.outBusinessNo()),
                () -> assertEquals(RebateTypeVO.INTEGRAL, integralOrder.rebateType()),
                () -> assertEquals("10", integralOrder.rebateConfig()),
                () -> assertEquals(USER_ID + "_integral_" + OUT_BUSINESS_NO, integralOrder.bizId()),
                () -> assertEquals(OUT_BUSINESS_NO, integralOrder.outBusinessNo())
        );

        SendRebateEvent skuEvent = aggregate.events().get(0);
        SendRebateEvent integralEvent = aggregate.events().get(1);
        assertAll(
                () -> assertEquals(skuOrder.bizId(), skuEvent.bizId()),
                () -> assertEquals(skuOrder.rebateType(), skuEvent.rebateType()),
                () -> assertEquals(skuOrder.rebateConfig(), skuEvent.rebateConfig()),
                () -> assertEquals(integralOrder.bizId(), integralEvent.bizId()),
                () -> assertEquals(integralOrder.rebateType(), integralEvent.rebateType()),
                () -> assertEquals(integralOrder.rebateConfig(), integralEvent.rebateConfig())
        );
    }

    /**
     * Given 行为没有配置返利规则，When 创建返利订单，Then 返回空结果且不保存聚合。
     */
    @Test
    void shouldReturnEmptyResultWhenNoRebateConfigExists() {
        // Given
        IBehaviorRebateRepository repository = mock(IBehaviorRebateRepository.class);
        BehaviorRebateServiceImpl service = new BehaviorRebateServiceImpl(repository);
        when(repository.queryBehaviorRebateConfig(BehaviorTypeVO.SIGN)).thenReturn(List.of());

        // When
        BehaviorRebateOrderResult result = service.createOrder(new BehaviorEntity(
                USER_ID,
                BehaviorTypeVO.SIGN,
                OUT_BUSINESS_NO
        ));

        // Then
        assertAll(
                () -> assertTrue(result.orderIds().isEmpty()),
                () -> assertTrue(result.eventIds().isEmpty())
        );
        verify(repository, never()).saveUserBehaviorRebateAggregate(any());
    }
}
