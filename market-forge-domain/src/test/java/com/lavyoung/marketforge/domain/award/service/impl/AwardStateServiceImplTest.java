package com.lavyoung.marketforge.domain.award.service.impl;

import com.lavyoung.marketforge.domain.award.model.entity.UserAwardRecordEntity;
import com.lavyoung.marketforge.domain.award.model.valobj.AwardStateVO;
import com.lavyoung.marketforge.domain.award.repository.IAwardRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * 用户奖品状态领域服务测试。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AwardStateServiceImplTest {

    private static final String USER_ID = "user-001";
    private static final String ORDER_ID = "order-001";

    @Mock
    private IAwardRepository awardRepository;

    /**
     * 创建状态的中奖记录应通过带当前状态的条件更新推进到待领取。
     */
    @Test
    void shouldAdvanceCreatedRecordToWaitClaim() {
        // Given
        AwardStateServiceImpl service = new AwardStateServiceImpl(awardRepository);
        when(awardRepository.queryUserAwardRecord(USER_ID, ORDER_ID))
                .thenReturn(Optional.of(record(AwardStateVO.CREATE)));
        when(awardRepository.updateAwardState(
                USER_ID, ORDER_ID, AwardStateVO.CREATE, AwardStateVO.WAIT_CLAIM))
                .thenReturn(true);

        // When
        service.prepareClaim(USER_ID, ORDER_ID);

        // Then
        verify(awardRepository).updateAwardState(
                USER_ID, ORDER_ID, AwardStateVO.CREATE, AwardStateVO.WAIT_CLAIM);
    }

    /**
     * 已离开创建状态的重复消息不得再次执行状态更新。
     */
    @Test
    void shouldIgnoreRecordThatAlreadyAdvanced() {
        // Given
        AwardStateServiceImpl service = new AwardStateServiceImpl(awardRepository);
        when(awardRepository.queryUserAwardRecord(USER_ID, ORDER_ID))
                .thenReturn(Optional.of(record(AwardStateVO.WAIT_CLAIM)));

        // When
        service.prepareClaim(USER_ID, ORDER_ID);

        // Then
        verify(awardRepository, never()).updateAwardState(
                USER_ID, ORDER_ID, AwardStateVO.CREATE, AwardStateVO.WAIT_CLAIM);
    }

    /**
     * CAS 更新失败且记录仍停留在创建状态时应抛出异常，使上层事务回滚并重试消息。
     */
    @Test
    void shouldFailWhenStateCannotBeAdvanced() {
        // Given
        AwardStateServiceImpl service = new AwardStateServiceImpl(awardRepository);
        when(awardRepository.queryUserAwardRecord(USER_ID, ORDER_ID))
                .thenReturn(Optional.of(record(AwardStateVO.CREATE)));
        when(awardRepository.updateAwardState(
                USER_ID, ORDER_ID, AwardStateVO.CREATE, AwardStateVO.WAIT_CLAIM))
                .thenReturn(false);

        // When / Then
        assertThrows(BusinessException.class, () -> service.prepareClaim(USER_ID, ORDER_ID));
    }

    /**
     * CAS 失败后若发现状态已被并发请求推进，则应视为幂等成功。
     */
    @Test
    void shouldAcceptConcurrentSuccessfulAdvance() {
        // Given
        AwardStateServiceImpl service = new AwardStateServiceImpl(awardRepository);
        when(awardRepository.queryUserAwardRecord(USER_ID, ORDER_ID))
                .thenReturn(Optional.of(record(AwardStateVO.CREATE)))
                .thenReturn(Optional.of(record(AwardStateVO.WAIT_CLAIM)));
        when(awardRepository.updateAwardState(
                USER_ID, ORDER_ID, AwardStateVO.CREATE, AwardStateVO.WAIT_CLAIM))
                .thenReturn(false);

        // When / Then
        assertDoesNotThrow(() -> service.prepareClaim(USER_ID, ORDER_ID));
    }

    private UserAwardRecordEntity record(AwardStateVO state) {
        return UserAwardRecordEntity.builder()
                .userId(USER_ID)
                .activityId(100001L)
                .strategyId(100006L)
                .orderId(ORDER_ID)
                .awardId(100011L)
                .awardTitle("测试奖品")
                .awardTime(LocalDateTime.of(2026, 9, 30, 12, 0))
                .awardState(state)
                .build();
    }
}
