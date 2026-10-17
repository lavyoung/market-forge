package com.lavyoung.marketforge.domain.award.service.impl;

import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardUnlockCountQueryVO;
import com.lavyoung.marketforge.domain.award.model.vo.AwardUnlockResultVO;
import com.lavyoung.marketforge.domain.award.repository.IAwardRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 奖品解锁领域服务单元测试。
 *
 * <p>验证领域服务能够基于仓储统计次数形成稳定的解锁结论，
 * 同时阻止非法门槛和非法仓储结果进入页面展示或抽奖规则链路。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/17
 */
@ExtendWith(MockitoExtension.class)
class AwardUnlockServiceImplTest {

    private static final String USER_ID = "user-001";
    private static final Long ACTIVITY_ID = 100_301L;
    private static final LocalDate STATISTICS_DATE = LocalDate.of(2026, 10, 17);

    @Mock
    private IAwardRepository awardRepository;

    private AwardUnlockServiceImpl awardUnlockService;
    private AwardUnlockCountQueryVO queryVO;

    /**
     * 为每个测试准备独立的领域服务和日范围查询条件。
     */
    @BeforeEach
    void setUp() {
        awardUnlockService = new AwardUnlockServiceImpl(awardRepository);
        queryVO = new AwardUnlockCountQueryVO(
                USER_ID,
                ACTIVITY_ID,
                AwardUnlockScopeVO.DAY,
                STATISTICS_DATE
        );
    }

    /**
     * Given 已完成次数小于门槛，When 判定解锁状态，Then 返回未解锁和准确的剩余次数。
     */
    @Test
    void shouldReturnLockedResultWhenCompletedCountIsBelowRequirement() {
        // Given
        when(awardRepository.countCompletedRaffles(queryVO)).thenReturn(2L);

        // When
        AwardUnlockResultVO result = awardUnlockService.evaluateUnlock(queryVO, 5);

        // Then
        assertAll(
                () -> assertEquals(5, result.requiredCount()),
                () -> assertEquals(2L, result.completedCount()),
                () -> assertFalse(result.unlocked()),
                () -> assertEquals(3, result.remainingCount())
        );
        verify(awardRepository).countCompletedRaffles(queryVO);
    }

    /**
     * Given 已完成次数等于门槛，When 判定解锁状态，Then 奖品刚好解锁且剩余次数为零。
     */
    @Test
    void shouldReturnUnlockedResultWhenCompletedCountEqualsRequirement() {
        // Given
        when(awardRepository.countCompletedRaffles(queryVO)).thenReturn(5L);

        // When
        AwardUnlockResultVO result = awardUnlockService.evaluateUnlock(queryVO, 5);

        // Then
        assertAll(
                () -> assertTrue(result.unlocked()),
                () -> assertEquals(0, result.remainingCount())
        );
        verify(awardRepository).countCompletedRaffles(queryVO);
    }

    /**
     * Given 已完成次数超过门槛，When 判定解锁状态，Then 保持已解锁且不产生负剩余次数。
     */
    @Test
    void shouldReturnUnlockedResultWhenCompletedCountExceedsRequirement() {
        // Given
        when(awardRepository.countCompletedRaffles(queryVO)).thenReturn(8L);

        // When
        AwardUnlockResultVO result = awardUnlockService.evaluateUnlock(queryVO, 5);

        // Then
        assertAll(
                () -> assertTrue(result.unlocked()),
                () -> assertEquals(0, result.remainingCount())
        );
        verify(awardRepository).countCompletedRaffles(queryVO);
    }

    /**
     * Given 奖品没有次数门槛，When 判定解锁状态，Then 即使没有抽奖记录也立即解锁。
     */
    @Test
    void shouldUnlockImmediatelyWhenRequiredCountIsZero() {
        // Given
        when(awardRepository.countCompletedRaffles(queryVO)).thenReturn(0L);

        // When
        AwardUnlockResultVO result = awardUnlockService.evaluateUnlock(queryVO, 0);

        // Then
        assertAll(
                () -> assertTrue(result.unlocked()),
                () -> assertEquals(0L, result.completedCount()),
                () -> assertEquals(0, result.remainingCount())
        );
        verify(awardRepository).countCompletedRaffles(queryVO);
    }

    /**
     * Given 解锁门槛为负数，When 判定解锁状态，Then 返回参数错误且不查询仓储。
     */
    @Test
    void shouldRejectNegativeRequiredCountWithoutQueryingRepository() {
        // When
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> awardUnlockService.evaluateUnlock(queryVO, -1)
        );

        // Then
        assertEquals(CommonResponseCode.PARAM_INVALID, exception.getResponseCode());
        verifyNoInteractions(awardRepository);
    }

    /**
     * Given 查询条件为空，When 判定解锁状态，Then 快速失败且不查询仓储。
     */
    @Test
    void shouldRejectNullQueryWithoutQueryingRepository() {
        // When & Then
        assertThrows(
                NullPointerException.class,
                () -> awardUnlockService.evaluateUnlock(null, 1)
        );
        verifyNoInteractions(awardRepository);
    }

    /**
     * Given 仓储返回不可能出现的负数，When 判定解锁状态，Then 暴露系统错误而不生成错误结果。
     */
    @Test
    void shouldRejectNegativeCompletedCountReturnedByRepository() {
        // Given
        when(awardRepository.countCompletedRaffles(queryVO)).thenReturn(-1L);

        // When
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> awardUnlockService.evaluateUnlock(queryVO, 5)
        );

        // Then
        assertEquals(CommonResponseCode.SYSTEM_ERROR, exception.getResponseCode());
        verify(awardRepository).countCompletedRaffles(queryVO);
    }

    /**
     * Given 多个奖品具有不同解锁门槛，When 批量判定，Then 只查询一次仓储并复用同一完成次数。
     */
    @Test
    void shouldEvaluateMultipleRequirementsWithSingleRepositoryQuery() {
        // Given
        when(awardRepository.countCompletedRaffles(queryVO)).thenReturn(3L);

        // When
        Map<Integer, AwardUnlockResultVO> results = awardUnlockService.evaluateUnlocks(
                queryVO,
                Set.of(2, 5, 8)
        );

        // Then
        assertAll(
                () -> assertTrue(results.get(2).unlocked()),
                () -> assertEquals(0, results.get(2).remainingCount()),
                () -> assertFalse(results.get(5).unlocked()),
                () -> assertEquals(2, results.get(5).remainingCount()),
                () -> assertFalse(results.get(8).unlocked()),
                () -> assertEquals(5, results.get(8).remainingCount())
        );
        verify(awardRepository).countCompletedRaffles(queryVO);
    }

    /**
     * Given 没有奖品配置次数锁，When 批量判定空门槛集合，Then 返回空结果且不访问仓储。
     */
    @Test
    void shouldReturnEmptyResultWithoutQueryingRepositoryWhenRequirementsAreEmpty() {
        // When
        Map<Integer, AwardUnlockResultVO> results = awardUnlockService.evaluateUnlocks(
                queryVO,
                Set.of()
        );

        // Then
        assertTrue(results.isEmpty());
        verifyNoInteractions(awardRepository);
    }
}
