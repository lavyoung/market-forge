package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.behavior.event.SendRebateEvent;
import com.lavyoung.marketforge.domain.behavior.model.aggregate.UserBehaviorRebateAggregate;
import com.lavyoung.marketforge.domain.behavior.model.entity.UserBehaviorRebateOrderEntity;
import com.lavyoung.marketforge.domain.behavior.model.vo.BehaviorTypeVO;
import com.lavyoung.marketforge.domain.behavior.model.vo.RebateTypeVO;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.TaskAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.behavior.BehaviorRebateConfigAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.behavior.UserBehaviorRebateOrderAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.behavior.IBehaviorRebateConfigDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.behavior.IUserBehaviorRebateOrderDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.mq.ITaskDao;
import com.lavyoung.marketforge.infrastructure.persistent.po.TaskPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.behavior.UserBehaviorRebateOrderPO;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.messaging.IntegrationEventCodec;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * 用户行为返利仓储测试。
 *
 * <p>重点覆盖返利订单和本地消息任务的事务落库边界。重复业务幂等号应转换为清晰的
 * 业务异常，不能把数据库唯一键异常直接暴露到应用层。</p>
 */
@ExtendWith(MockitoExtension.class)
class BehaviorRebateRepositoryTest {

    private static final String USER_ID = "user-001";
    private static final String BIZ_ID = "user-001_integral_sign-20261011-user-001";

    @Mock
    private IBehaviorRebateConfigDao behaviorRebateConfigDao;

    @Mock
    private IUserBehaviorRebateOrderDao userBehaviorRebateOrderDao;

    @Mock
    private BehaviorRebateConfigAssembler behaviorRebateConfigAssembler;

    @Mock
    private UserBehaviorRebateOrderAssembler userBehaviorRebateOrderAssembler;

    @Mock
    private ITaskDao taskDao;

    @Mock
    private TaskAssembler taskAssembler;

    @Mock
    private IntegrationEventCodec eventCodec;

    /**
     * Given 返利订单业务幂等号已存在，When 保存返利聚合，Then 应抛业务重复异常且不创建本地消息任务。
     */
    @Test
    void shouldTranslateDuplicateRebateOrderToBusinessException() {
        // Given
        BehaviorRebateRepository repository = repository();
        UserBehaviorRebateOrderEntity order = integralOrder();
        UserBehaviorRebateOrderPO orderPO = new UserBehaviorRebateOrderPO();
        when(userBehaviorRebateOrderAssembler.toPO(order)).thenReturn(orderPO);
        when(userBehaviorRebateOrderDao.insert(orderPO))
                .thenThrow(new DuplicateKeyException("duplicated behavior rebate biz id"));

        UserBehaviorRebateAggregate aggregate = new UserBehaviorRebateAggregate(
                USER_ID,
                List.of(order),
                List.of(new SendRebateEvent(USER_ID, BIZ_ID, RebateTypeVO.INTEGRAL, "10"))
        );

        // When
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> repository.saveUserBehaviorRebateAggregate(aggregate)
        );

        // Then
        assertEquals(BusinessResponseCode.BEHAVIOR_REBATE_ORDER_DUPLICATED, exception.getResponseCode());
        verify(taskDao, never()).insert(any(TaskPO.class));
        verifyNoInteractions(taskAssembler, eventCodec);
    }

    private BehaviorRebateRepository repository() {
        return new BehaviorRebateRepository(
                behaviorRebateConfigDao,
                userBehaviorRebateOrderDao,
                behaviorRebateConfigAssembler,
                userBehaviorRebateOrderAssembler,
                taskDao,
                taskAssembler,
                eventCodec
        );
    }

    private UserBehaviorRebateOrderEntity integralOrder() {
        return new UserBehaviorRebateOrderEntity(
                USER_ID,
                "rebate-order-001",
                BehaviorTypeVO.SIGN,
                RebateTypeVO.INTEGRAL,
                "10",
                BIZ_ID,
                "sign-20261011-user-001",
                LocalDateTime.of(2026, 10, 11, 21, 40)
        );
    }
}
