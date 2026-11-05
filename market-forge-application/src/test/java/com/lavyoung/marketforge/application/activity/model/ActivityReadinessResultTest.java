package com.lavyoung.marketforge.application.activity.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 活动开放就绪检查应用结果测试。
 *
 * <p>验证就绪状态由错误集合推导，并确保问题集合在结果创建后不可修改。</p>
 */
class ActivityReadinessResultTest {

    private static final LocalDateTime CHECKED_AT = LocalDateTime.of(2026, 11, 5, 12, 0);

    @Test
    void shouldCreateReadyImmutableResult() {
        // Given
        List<ActivityReadinessIssueResult> warnings = new ArrayList<>();
        warnings.add(new ActivityReadinessIssueResult("activity.time.started", "beginDateTime", "活动已经开始"));

        // When
        ActivityReadinessResult result = new ActivityReadinessResult(
                100301L, CHECKED_AT, List.of(), warnings, List.of());
        warnings.clear();

        // Then
        assertAll(
                () -> assertTrue(result.ready()),
                () -> assertEquals(1, result.warnings().size()),
                () -> assertThrows(UnsupportedOperationException.class,
                        () -> result.warnings().add(new ActivityReadinessIssueResult("code", "path", "message")))
        );
    }

    @Test
    void shouldNotBeReadyWhenErrorsExist() {
        // Given
        ActivityReadinessIssueResult error = new ActivityReadinessIssueResult(
                "activity.sku.missing", "skus", "活动至少需要配置一个额度 SKU");

        // When
        ActivityReadinessResult result = new ActivityReadinessResult(
                100301L, CHECKED_AT, List.of(error), List.of(), List.of());

        // Then
        assertFalse(result.ready());
    }

    @Test
    void shouldRejectInvalidRequiredFields() {
        // When & Then
        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> new ActivityReadinessResult(null, CHECKED_AT, List.of(), List.of(), List.of())),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ActivityReadinessResult(0L, CHECKED_AT, List.of(), List.of(), List.of())),
                () -> assertThrows(NullPointerException.class,
                        () -> new ActivityReadinessResult(100301L, null, List.of(), List.of(), List.of())),
                () -> assertThrows(NullPointerException.class,
                        () -> new ActivityReadinessResult(100301L, CHECKED_AT, null, List.of(), List.of()))
        );
    }
}
