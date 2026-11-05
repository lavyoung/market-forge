package com.lavyoung.marketforge.domain.activity.service.impl;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityDefinitionStateVO;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityReadinessIssueVO;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityReadinessReportVO;
import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityCountDefinitionRepository;
import com.lavyoung.marketforge.domain.activity.repository.IActivitySkuDefinitionRepository;
import com.lavyoung.marketforge.domain.strategy.model.aggregate.StrategyDefinitionAggregate;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyDefinitionStateVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyProbabilityPreviewVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyReadinessIssueVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyReadinessReportVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyDefinitionRepository;
import com.lavyoung.marketforge.domain.strategy.service.IStrategyDefinitionReadinessChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 活动定义开放就绪检查器测试。
 *
 * <p>验证活动时间、次数配置、SKU 可用性、策略生命周期和策略检查报告能够被
 * 一次性聚合，并确保关联次数配置只执行一次批量查询。</p>
 */
@ExtendWith(MockitoExtension.class)
class ActivityDefinitionReadinessCheckerTest {

    private static final Long ACTIVITY_ID = 100301L;
    private static final Long ACTIVITY_COUNT_ID = 100302L;
    private static final Long STRATEGY_ID = 100303L;
    private static final LocalDateTime CHECKED_AT = LocalDateTime.of(2026, 11, 4, 12, 0);

    @Mock
    private IActivityCountDefinitionRepository activityCountDefinitionRepository;

    @Mock
    private IActivitySkuDefinitionRepository activitySkuDefinitionRepository;

    @Mock
    private IStrategyDefinitionRepository strategyDefinitionRepository;

    @Mock
    private IStrategyDefinitionReadinessChecker strategyDefinitionReadinessChecker;

    private ActivityDefinitionReadinessChecker checker;

    @BeforeEach
    void setUp() {
        checker = new ActivityDefinitionReadinessChecker(activityCountDefinitionRepository,
                activitySkuDefinitionRepository, strategyDefinitionRepository,
                strategyDefinitionReadinessChecker);
    }

    @Test
    void shouldBeReadyWhenAllReferencesAreUsable() {
        // Given
        ActivityDefinitionEntity definition = activity(
                ActivityDefinitionStateVO.DRAFT, CHECKED_AT.plusDays(1), CHECKED_AT.plusDays(10));
        StrategyDefinitionAggregate strategy = strategy(StrategyDefinitionStateVO.ACTIVE);
        stubReadyReferences(definition, strategy);

        // When
        ActivityReadinessReportVO report = checker.check(definition, CHECKED_AT);

        // Then
        assertAll(
                () -> assertTrue(report.ready()),
                () -> assertTrue(report.errors().isEmpty()),
                () -> assertTrue(report.warnings().isEmpty()),
                () -> assertTrue(report.suggestions().isEmpty()),
                () -> verify(activityCountDefinitionRepository).queryByActivityCountIds(Set.of(ACTIVITY_COUNT_ID))
        );
    }

    @Test
    void shouldRemainReadyForPublishingWhenActivityIsOpen() {
        // Given
        ActivityDefinitionEntity definition = activity(
                ActivityDefinitionStateVO.OPEN, CHECKED_AT.plusDays(1), CHECKED_AT.plusDays(10));
        StrategyDefinitionAggregate strategy = strategy(StrategyDefinitionStateVO.ACTIVE);
        stubReadyReferences(definition, strategy);

        // When
        ActivityReadinessReportVO report = checker.check(definition, CHECKED_AT);

        // Then
        assertAll(
                () -> assertTrue(report.ready()),
                () -> assertTrue(report.errors().isEmpty())
        );
    }

    @Test
    void shouldWarnWhenActivityHasAlreadyStarted() {
        // Given
        ActivityDefinitionEntity definition = activity(
                ActivityDefinitionStateVO.DRAFT, CHECKED_AT.minusDays(1), CHECKED_AT.plusDays(10));
        StrategyDefinitionAggregate strategy = strategy(StrategyDefinitionStateVO.ACTIVE);
        stubReadyReferences(definition, strategy);

        // When
        ActivityReadinessReportVO report = checker.check(definition, CHECKED_AT);

        // Then
        assertAll(
                () -> assertTrue(report.ready()),
                () -> assertEquals(List.of("activity.time.already-started"), codes(report.warnings()))
        );
    }

    @Test
    void shouldReportClosedActivityStateAndExpiredTime() {
        // Given
        ActivityDefinitionEntity definition = activity(
                ActivityDefinitionStateVO.CLOSE, CHECKED_AT.minusDays(10), CHECKED_AT.minusDays(1));
        StrategyDefinitionAggregate strategy = strategy(StrategyDefinitionStateVO.ACTIVE);
        stubReadyReferences(definition, strategy);

        // When
        ActivityReadinessReportVO report = checker.check(definition, CHECKED_AT);

        // Then
        assertEquals(Set.of("activity.state.closed", "activity.time.expired"), Set.copyOf(codes(report.errors())));
    }

    @Test
    void shouldReportMissingActivityCountAndSku() {
        // Given
        ActivityDefinitionEntity definition = activity(
                ActivityDefinitionStateVO.DRAFT, CHECKED_AT.plusDays(1), CHECKED_AT.plusDays(10));
        StrategyDefinitionAggregate strategy = strategy(StrategyDefinitionStateVO.ACTIVE);
        when(activitySkuDefinitionRepository.queryByActivityId(ACTIVITY_ID)).thenReturn(List.of());
        when(activityCountDefinitionRepository.queryByActivityCountIds(Set.of(ACTIVITY_COUNT_ID))).thenReturn(List.of());
        when(strategyDefinitionRepository.queryByStrategyId(STRATEGY_ID)).thenReturn(Optional.of(strategy));
        when(strategyDefinitionReadinessChecker.check(strategy)).thenReturn(readyStrategyReport());

        // When
        ActivityReadinessReportVO report = checker.check(definition, CHECKED_AT);

        // Then
        assertEquals(Set.of("activity.count.missing", "activity.sku.missing"), Set.copyOf(codes(report.errors())));
    }

    @Test
    void shouldReportSkuRelationshipStockAndExchangeProblems() {
        // Given
        ActivityDefinitionEntity definition = activity(
                ActivityDefinitionStateVO.DRAFT, CHECKED_AT.plusDays(1), CHECKED_AT.plusDays(10));
        ActivitySkuDefinitionEntity invalidSku = new ActivitySkuDefinitionEntity(
                9011L, 999999L, 100304L, 100, 0, null, 1, "admin", "admin", null, null);
        ActivityCountDefinitionEntity activityCount = countDefinition();
        StrategyDefinitionAggregate strategy = strategy(StrategyDefinitionStateVO.ACTIVE);
        when(activitySkuDefinitionRepository.queryByActivityId(ACTIVITY_ID)).thenReturn(List.of(invalidSku));
        when(activityCountDefinitionRepository.queryByActivityCountIds(Set.of(ACTIVITY_COUNT_ID, 100304L)))
                .thenReturn(List.of(activityCount));
        when(strategyDefinitionRepository.queryByStrategyId(STRATEGY_ID)).thenReturn(Optional.of(strategy));
        when(strategyDefinitionReadinessChecker.check(strategy)).thenReturn(readyStrategyReport());

        // When
        ActivityReadinessReportVO report = checker.check(definition, CHECKED_AT);

        // Then
        assertAll(
                () -> assertEquals(Set.of("activity.sku.activity-mismatch", "activity.sku.count-missing",
                        "activity.sku.none-usable"), Set.copyOf(codes(report.errors()))),
                () -> assertEquals(List.of("activity.sku.stock-empty"), codes(report.warnings())),
                () -> assertEquals(List.of("activity.sku.credit-exchange-disabled"), codes(report.suggestions())),
                () -> assertEquals("skus[0].activityId", report.errors().get(0).path())
        );
    }

    @Test
    void shouldReportMissingStrategyWithoutCallingStrategyChecker() {
        // Given
        ActivityDefinitionEntity definition = activity(
                ActivityDefinitionStateVO.DRAFT, CHECKED_AT.plusDays(1), CHECKED_AT.plusDays(10));
        stubReadySkuAndCount();
        when(strategyDefinitionRepository.queryByStrategyId(STRATEGY_ID)).thenReturn(Optional.empty());

        // When
        ActivityReadinessReportVO report = checker.check(definition, CHECKED_AT);

        // Then
        assertAll(
                () -> assertEquals(List.of("activity.strategy.missing"), codes(report.errors())),
                () -> verifyNoInteractions(strategyDefinitionReadinessChecker)
        );
    }

    @Test
    void shouldRejectInactiveStrategyAndMergeStrategyIssues() {
        // Given
        ActivityDefinitionEntity definition = activity(
                ActivityDefinitionStateVO.DRAFT, CHECKED_AT.plusDays(1), CHECKED_AT.plusDays(10));
        StrategyDefinitionAggregate strategy = strategy(StrategyDefinitionStateVO.DRAFT);
        StrategyReadinessReportVO strategyReport = new StrategyReadinessReportVO(
                STRATEGY_ID,
                List.of(new StrategyReadinessIssueVO("strategy.rate.invalid", "awards[0].awardRate", "奖品概率不合法")),
                List.of(new StrategyReadinessIssueVO("strategy.stock.warning", "strategy", "策略库存需要确认")),
                List.of(new StrategyReadinessIssueVO("strategy.award.suggestion", "awards", "建议补充奖品")),
                StrategyProbabilityPreviewVO.unavailable());
        stubReadySkuAndCount();
        when(strategyDefinitionRepository.queryByStrategyId(STRATEGY_ID)).thenReturn(Optional.of(strategy));
        when(strategyDefinitionReadinessChecker.check(strategy)).thenReturn(strategyReport);

        // When
        ActivityReadinessReportVO report = checker.check(definition, CHECKED_AT);

        // Then
        assertAll(
                () -> assertEquals(Set.of("activity.strategy.not-active", "strategy.rate.invalid"),
                        Set.copyOf(codes(report.errors()))),
                () -> assertEquals("strategy.awards[0].awardRate", report.errors().get(1).path()),
                () -> assertEquals("strategy", report.warnings().get(0).path()),
                () -> assertEquals("strategy.awards", report.suggestions().get(0).path())
        );
    }

    @Test
    void shouldRejectNullArgumentsBeforeReadingRepositories() {
        // Given
        ActivityDefinitionEntity definition = activity(
                ActivityDefinitionStateVO.DRAFT, CHECKED_AT.plusDays(1), CHECKED_AT.plusDays(10));

        // When
        NullPointerException nullDefinition = assertThrows(NullPointerException.class,
                () -> checker.check(null, CHECKED_AT));
        NullPointerException nullCheckedAt = assertThrows(NullPointerException.class,
                () -> checker.check(definition, null));

        // Then
        assertAll(
                () -> assertEquals("definition must not be null", nullDefinition.getMessage()),
                () -> assertEquals("checkedAt must not be null", nullCheckedAt.getMessage()),
                () -> verifyNoInteractions(activityCountDefinitionRepository, activitySkuDefinitionRepository,
                        strategyDefinitionRepository, strategyDefinitionReadinessChecker)
        );
    }

    private void stubReadyReferences(ActivityDefinitionEntity definition, StrategyDefinitionAggregate strategy) {
        stubReadySkuAndCount();
        when(strategyDefinitionRepository.queryByStrategyId(definition.strategyId())).thenReturn(Optional.of(strategy));
        when(strategyDefinitionReadinessChecker.check(strategy)).thenReturn(readyStrategyReport());
    }

    private void stubReadySkuAndCount() {
        ActivitySkuDefinitionEntity sku = new ActivitySkuDefinitionEntity(
                9011L, ACTIVITY_ID, ACTIVITY_COUNT_ID, 100, 80, 50L, 1, "admin", "admin", null, null);
        when(activitySkuDefinitionRepository.queryByActivityId(ACTIVITY_ID)).thenReturn(List.of(sku));
        when(activityCountDefinitionRepository.queryByActivityCountIds(Set.of(ACTIVITY_COUNT_ID)))
                .thenReturn(List.of(countDefinition()));
    }

    private ActivityDefinitionEntity activity(
            ActivityDefinitionStateVO state,
            LocalDateTime beginDateTime,
            LocalDateTime endDateTime
    ) {
        return new ActivityDefinitionEntity(
                ACTIVITY_ID, "测试活动", "活动就绪检查", beginDateTime, endDateTime,
                ACTIVITY_COUNT_ID, STRATEGY_ID, AwardUnlockScopeVO.TOTAL, state, 1,
                "admin", "admin", null, null);
    }

    private ActivityCountDefinitionEntity countDefinition() {
        return new ActivityCountDefinitionEntity(
                ACTIVITY_COUNT_ID, "基础次数包", 100, 10, 50, 1, "admin", "admin", null, null);
    }

    private StrategyDefinitionAggregate strategy(StrategyDefinitionStateVO state) {
        return new StrategyDefinitionAggregate(
                STRATEGY_ID, "测试策略", null, state, 1, "admin", "admin", null, null, List.of(), List.of());
    }

    private StrategyReadinessReportVO readyStrategyReport() {
        return new StrategyReadinessReportVO(
                STRATEGY_ID, List.of(), List.of(), List.of(), StrategyProbabilityPreviewVO.unavailable());
    }

    private List<String> codes(List<ActivityReadinessIssueVO> issues) {
        return issues.stream().map(ActivityReadinessIssueVO::code).toList();
    }
}
