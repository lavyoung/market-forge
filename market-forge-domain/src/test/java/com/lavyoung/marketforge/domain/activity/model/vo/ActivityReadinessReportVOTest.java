package com.lavyoung.marketforge.domain.activity.model.vo;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ActivityReadinessIssueVO} 和 {@link ActivityReadinessReportVO} 单元测试。
 *
 * <p>验证活动就绪问题会显式规范化文本，报告根据阻断错误实时推导是否就绪，
 * 并通过不可变集合防止检查结果在创建后被调用方篡改。</p>
 */
class ActivityReadinessReportVOTest {

    @Test
    void shouldNormalizeReadinessIssueText() {
        // Given / When
        ActivityReadinessIssueVO issue = new ActivityReadinessIssueVO(
                "  activity.sku.missing  ",
                "  skus  ",
                "  至少配置一个活动 SKU  "
        );

        // Then
        assertEquals("activity.sku.missing", issue.code());
        assertEquals("skus", issue.path());
        assertEquals("至少配置一个活动 SKU", issue.message());
    }

    @Test
    void shouldRejectInvalidReadinessIssueText() {
        // Given / When / Then
        assertThrows(NullPointerException.class,
                () -> new ActivityReadinessIssueVO(null, "activity", "配置错误"));
        assertThrows(IllegalArgumentException.class,
                () -> new ActivityReadinessIssueVO(" ", "activity", "配置错误"));
        assertThrows(IllegalArgumentException.class,
                () -> new ActivityReadinessIssueVO("activity.invalid", " ", "配置错误"));
        assertThrows(IllegalArgumentException.class,
                () -> new ActivityReadinessIssueVO("activity.invalid", "activity", " "));
    }

    @Test
    void shouldBeReadyWhenThereAreNoBlockingErrors() {
        // Given
        ActivityReadinessIssueVO warning = new ActivityReadinessIssueVO(
                "activity.time.start-in-future",
                "beginDateTime",
                "活动尚未开始"
        );

        // When
        ActivityReadinessReportVO report = new ActivityReadinessReportVO(
                100301L,
                List.of(),
                List.of(warning),
                List.of()
        );

        // Then
        assertTrue(report.ready());
        assertEquals(List.of(warning), report.warnings());
    }

    @Test
    void shouldNotBeReadyWhenBlockingErrorExists() {
        // Given
        ActivityReadinessIssueVO error = new ActivityReadinessIssueVO(
                "activity.strategy.not-active",
                "strategyId",
                "抽奖策略尚未生效"
        );

        // When
        ActivityReadinessReportVO report = new ActivityReadinessReportVO(
                100301L,
                List.of(error),
                List.of(),
                List.of()
        );

        // Then
        assertFalse(report.ready());
        assertEquals(List.of(error), report.errors());
    }

    @Test
    void shouldCopyIssueCollectionsAsImmutableValues() {
        // Given
        ActivityReadinessIssueVO warning = new ActivityReadinessIssueVO(
                "activity.sku.low-stock",
                "skus[0].stockCountSurplus",
                "SKU 剩余库存较低"
        );
        List<ActivityReadinessIssueVO> warnings = new ArrayList<>();
        warnings.add(warning);

        // When
        ActivityReadinessReportVO report = new ActivityReadinessReportVO(
                100301L,
                List.of(),
                warnings,
                List.of()
        );
        warnings.clear();

        // Then
        assertEquals(List.of(warning), report.warnings());
        assertThrows(UnsupportedOperationException.class,
                () -> report.warnings().add(warning));
    }

    @Test
    void shouldRejectInvalidReportStructure() {
        // Given / When / Then
        assertThrows(NullPointerException.class,
                () -> new ActivityReadinessReportVO(null, List.of(), List.of(), List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new ActivityReadinessReportVO(0L, List.of(), List.of(), List.of()));
        assertThrows(NullPointerException.class,
                () -> new ActivityReadinessReportVO(100301L, null, List.of(), List.of()));

        List<ActivityReadinessIssueVO> issuesWithNull = new ArrayList<>();
        issuesWithNull.add(null);
        assertThrows(NullPointerException.class,
                () -> new ActivityReadinessReportVO(100301L, issuesWithNull, List.of(), List.of()));
    }
}
