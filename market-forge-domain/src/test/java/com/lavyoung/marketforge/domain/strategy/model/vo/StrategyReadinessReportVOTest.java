package com.lavyoung.marketforge.domain.strategy.model.vo;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 策略就绪报告值对象测试。
 *
 * <p>验证问题文本规范化、概率区间覆盖、不可用预览和报告就绪状态等不变量，
 * 防止后续就绪检查器生成前后矛盾的运营结果。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class StrategyReadinessReportVOTest {

    private static final Long STRATEGY_ID = 900_901_001L;
    private static final Long FIRST_AWARD_ID = 900_901_011L;
    private static final Long SECOND_AWARD_ID = 900_901_012L;

    /**
     * Given 带有首尾空格的问题字段，When 创建问题对象，Then 应得到规范化文本。
     */
    @Test
    void shouldNormalizeReadinessIssueText() {
        // Given & When
        StrategyReadinessIssueVO issue = new StrategyReadinessIssueVO(
                " strategy.award.missing ", " awards[0].awardId ", " 奖品不存在 ");

        // Then
        assertAll(
                () -> assertEquals("strategy.award.missing", issue.code()),
                () -> assertEquals("awards[0].awardId", issue.path()),
                () -> assertEquals("奖品不存在", issue.message())
        );
    }

    /**
     * Given 空白问题编码，When 创建问题对象，Then 应拒绝无法稳定识别的问题。
     */
    @Test
    void shouldRejectBlankReadinessIssueCode() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> new StrategyReadinessIssueVO(" ", "strategy", "配置错误"));
    }

    /**
     * Given 两个连续奖品区间，When 创建可用概率预览，Then 应保留完整覆盖关系。
     */
    @Test
    void shouldCreateAvailableProbabilityPreview() {
        // Given
        StrategyProbabilityIntervalVO first = interval(FIRST_AWARD_ID, 30, 0, 30, "0.30000000");
        StrategyProbabilityIntervalVO second = interval(SECOND_AWARD_ID, 70, 30, 100, "0.70000000");

        // When
        StrategyProbabilityPreviewVO preview = new StrategyProbabilityPreviewVO(true, 100, List.of(first, second));

        // Then
        assertAll(
                () -> assertTrue(preview.available()),
                () -> assertEquals(100, preview.totalWeight()),
                () -> assertEquals(List.of(first, second), preview.intervals())
        );
    }

    /**
     * Given 区间起点没有承接上一区间终点，When 创建概率预览，Then 应拒绝存在空洞的概率空间。
     */
    @Test
    void shouldRejectProbabilityPreviewWithGap() {
        // Given
        StrategyProbabilityIntervalVO first = interval(FIRST_AWARD_ID, 30, 0, 30, "0.30000000");
        StrategyProbabilityIntervalVO second = interval(SECOND_AWARD_ID, 60, 40, 100, "0.60000000");

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> new StrategyProbabilityPreviewVO(true, 100, List.of(first, second)));
    }

    /**
     * Given 区间长度与权重不一致，When 创建奖品概率区间，Then 应拒绝错误预览数据。
     */
    @Test
    void shouldRejectIntervalWhoseLengthDiffersFromWeight() {
        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> interval(FIRST_AWARD_ID, 20, 0, 30, "0.20000000"));
    }

    /**
     * Given 概率无法生成，When 创建不可用预览，Then 应返回明确的空概率空间。
     */
    @Test
    void shouldCreateUnavailableProbabilityPreview() {
        // When
        StrategyProbabilityPreviewVO preview = StrategyProbabilityPreviewVO.unavailable();

        // Then
        assertAll(
                () -> assertFalse(preview.available()),
                () -> assertEquals(0, preview.totalWeight()),
                () -> assertTrue(preview.intervals().isEmpty())
        );
    }

    /**
     * Given 不可用状态却携带概率数据，When 创建预览，Then 应拒绝自相矛盾的状态。
     */
    @Test
    void shouldRejectUnavailablePreviewWithProbabilityData() {
        // Given
        StrategyProbabilityIntervalVO interval = interval(FIRST_AWARD_ID, 100, 0, 100, "1.00000000");

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> new StrategyProbabilityPreviewVO(false, 100, List.of(interval)));
    }

    /**
     * Given 没有阻断错误的检查结果，When 创建报告，Then 策略应处于可发布状态且集合不可变。
     */
    @Test
    void shouldCreateReadyReportWithImmutableCollections() {
        // Given
        List<StrategyReadinessIssueVO> warnings = new ArrayList<>();
        warnings.add(new StrategyReadinessIssueVO(
                "strategy.probability.concentrated", "awards", "单个奖品权重占比较高"));

        // When
        StrategyReadinessReportVO report = new StrategyReadinessReportVO(
                STRATEGY_ID, List.of(), warnings, List.of(), StrategyProbabilityPreviewVO.unavailable());
        warnings.clear();

        // Then
        assertAll(
                () -> assertTrue(report.ready()),
                () -> assertEquals(1, report.warnings().size()),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> report.warnings().add(new StrategyReadinessIssueVO("code", "path", "message")))
        );
    }

    /**
     * Given 存在阻断错误的检查结果，When 查询就绪状态，Then 策略不得发布。
     */
    @Test
    void shouldMarkReportNotReadyWhenErrorsExist() {
        // Given
        StrategyReadinessIssueVO error = new StrategyReadinessIssueVO(
                "strategy.award.missing", "awards", "策略至少需要一个奖品");

        // When
        StrategyReadinessReportVO report = new StrategyReadinessReportVO(
                STRATEGY_ID, List.of(error), List.of(), List.of(), StrategyProbabilityPreviewVO.unavailable());

        // Then
        assertFalse(report.ready());
    }

    private StrategyProbabilityIntervalVO interval(Long awardId, int weight, int start, int end, String probability) {
        return new StrategyProbabilityIntervalVO(awardId, weight, start, end, new BigDecimal(probability));
    }
}
