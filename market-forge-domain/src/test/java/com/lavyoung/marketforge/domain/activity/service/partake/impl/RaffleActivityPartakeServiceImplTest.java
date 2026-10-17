package com.lavyoung.marketforge.domain.activity.service.partake.impl;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityOrderEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.PartakeRaffleActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStateVO;
import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import com.lavyoung.marketforge.domain.activity.model.vo.UserRaffleOrderStateVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * 抽奖参与服务上下文恢复测试。
 *
 * <p>验证数据库中的未消费订单被复用时，服务会使用当前活动配置
 * 补齐不会持久化的解锁范围和活动结束时间。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/17
 */
class RaffleActivityPartakeServiceImplTest {

    private static final String USER_ID = "user-001";
    private static final Long ACTIVITY_ID = 100_301L;
    private static final Long SKU = 9_011L;

    /**
     * Given 数据库存在缺少运行上下文的未消费订单，When 再次参与活动，Then 使用当前活动配置补齐上下文。
     */
    @Test
    void shouldRestoreActivityContextWhenReusingPersistedOrder() {
        // Given
        IActivityRepository repository = mock(IActivityRepository.class);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime activityEndTime = now.plusDays(7);
        PartakeRaffleActivityEntity partake = new PartakeRaffleActivityEntity(USER_ID, SKU, ACTIVITY_ID);
        ActivitySkuEntity sku = ActivitySkuEntity.builder()
                .sku(SKU)
                .activityId(ACTIVITY_ID)
                .build();
        ActivityEntity activity = ActivityEntity.builder()
                .activityId(ACTIVITY_ID)
                .activityName("九宫格活动抽奖")
                .strategyId(900_901_001L)
                .awardUnlockScope(AwardUnlockScopeVO.TOTAL)
                .beginDateTime(now.minusDays(1))
                .endDateTime(activityEndTime)
                .state(ActivityStateVO.OPEN.getCode())
                .build();
        ActivityOrderEntity persistedOrder = ActivityOrderEntity.builder()
                .userId(USER_ID)
                .activityId(ACTIVITY_ID)
                .sku(SKU)
                .strategyId(900_901_001L)
                .orderId("RO1003010001")
                .orderTime(now.minusMinutes(1))
                .state(UserRaffleOrderStateVO.CREATE.getCode())
                .build();
        when(repository.queryActivitySku(SKU)).thenReturn(sku);
        when(repository.getActivityEntityByIdActivityId(ACTIVITY_ID)).thenReturn(activity);
        when(repository.queryNotUsedRaffleOrder(partake)).thenReturn(Optional.of(persistedOrder));
        RaffleActivityPartakeServiceImpl service = new RaffleActivityPartakeServiceImpl(repository);

        // When
        ActivityOrderEntity result = service.createRaffleOrder(partake);

        // Then
        assertAll(
                () -> assertEquals(persistedOrder.orderId(), result.orderId()),
                () -> assertEquals(AwardUnlockScopeVO.TOTAL, result.awardUnlockScope()),
                () -> assertEquals(activityEndTime, result.endDateTime())
        );
        verify(repository).queryNotUsedRaffleOrder(partake);
    }
}
