package com.lavyoung.marketforge.application.activity.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 活动展台发布任务应用结果测试。
 *
 * <p>验证合法发布视图可以承载处理中状态，并覆盖必填字段、展台版本和尝试次数边界。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/06
 */
class ActivityStagePublicationResultTest {

    /**
     * 验证处理中结果允许失败信息和成功时间为空。
     */
    @Test
    void shouldCreateProcessingPublicationResult() {
        // Given
        LocalDateTime createTime = LocalDateTime.of(2026, 11, 6, 11, 0);

        // When
        ActivityStagePublicationResult result = result(3, 1, "processing", "checking", createTime);

        // Then
        assertAll(
                () -> assertEquals(910001L, result.publicationId()),
                () -> assertEquals("processing", result.state()),
                () -> assertEquals("checking", result.phase()),
                () -> assertNull(result.failureCode()),
                () -> assertNull(result.publishedAt())
        );
    }

    /**
     * 验证展台版本必须为正数。
     */
    @Test
    void shouldRejectNonPositiveStageVersion() {
        // Given
        LocalDateTime createTime = LocalDateTime.of(2026, 11, 6, 11, 0);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> result(0, 1, "processing", "checking", createTime));
    }

    /**
     * 验证发布尝试次数必须为正数。
     */
    @Test
    void shouldRejectNonPositiveAttemptCount() {
        // Given
        LocalDateTime createTime = LocalDateTime.of(2026, 11, 6, 11, 0);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> result(3, 0, "processing", "checking", createTime));
    }

    /**
     * 验证状态等必填展示字段不能为空。
     */
    @Test
    void shouldRejectNullRequiredField() {
        // Given
        LocalDateTime createTime = LocalDateTime.of(2026, 11, 6, 11, 0);

        // When & Then
        assertThrows(NullPointerException.class,
                () -> result(3, 1, null, "checking", createTime));
    }

    private ActivityStagePublicationResult result(int stageVersion, int attemptCount, String state,
                                                  String phase, LocalDateTime createTime) {
        return new ActivityStagePublicationResult(
                910001L, 900001L, stageVersion, 100001L, "wechat", "campaign", state, phase,
                attemptCount, "operator-001", null, null, null, createTime, createTime);
    }
}
