package com.lavyoung.marketforge.domain.activity.model.entity;

import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStagePublicationPhaseVO;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStagePublicationStateVO;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStageStateVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 活动展台发布记录测试。
 *
 * <p>验证首次发布、阶段推进、失败记录、失败重试和成功终态，并覆盖公开构造器
 * 以及领域行为对非法状态组合的防御。</p>
 */
class ActivityStagePublicationEntityTest {

    private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 11, 5, 10, 0);

    @Test
    void shouldStartPublicationFromDraftStageSnapshot() {
        // Given
        ActivityStageEntity stage = stage(ActivityStageStateVO.CREATE, 3);

        // When
        ActivityStagePublicationEntity publication = ActivityStagePublicationEntity.start(
                800001L, stage, "operator-001", STARTED_AT);

        // Then
        assertAll(
                () -> assertEquals(800001L, publication.publicationId()),
                () -> assertEquals(stage.stageId(), publication.stageId()),
                () -> assertEquals(stage.version(), publication.stageVersion()),
                () -> assertEquals(stage.activityId(), publication.activityId()),
                () -> assertEquals(ActivityStagePublicationStateVO.PROCESSING, publication.state()),
                () -> assertEquals(ActivityStagePublicationPhaseVO.CHECKING, publication.phase()),
                () -> assertEquals(1, publication.attemptCount()),
                () -> assertNull(publication.failureCode()),
                () -> assertNull(publication.publishedAt()),
                () -> assertEquals(STARTED_AT, publication.createTime()),
                () -> assertEquals(STARTED_AT, publication.updateTime())
        );
    }

    @Test
    void shouldAdvanceThroughPreheatingAndSwitchingWithoutChangingSnapshot() {
        // Given
        ActivityStagePublicationEntity checking = start();
        LocalDateTime preheatingAt = STARTED_AT.plusMinutes(1);
        LocalDateTime switchingAt = STARTED_AT.plusMinutes(2);

        // When
        ActivityStagePublicationEntity preheating = checking.advance(
                ActivityStagePublicationPhaseVO.PREHEATING, "operator-002", preheatingAt);
        ActivityStagePublicationEntity switching = preheating.advance(
                ActivityStagePublicationPhaseVO.SWITCHING, "operator-002", switchingAt);

        // Then
        assertAll(
                () -> assertEquals(ActivityStagePublicationPhaseVO.PREHEATING, preheating.phase()),
                () -> assertEquals(ActivityStagePublicationPhaseVO.SWITCHING, switching.phase()),
                () -> assertEquals(checking.publicationId(), switching.publicationId()),
                () -> assertEquals(checking.stageVersion(), switching.stageVersion()),
                () -> assertEquals(checking.activityId(), switching.activityId()),
                () -> assertEquals(checking.createTime(), switching.createTime()),
                () -> assertEquals(switchingAt, switching.updateTime()),
                () -> assertEquals("operator-002", switching.operator())
        );
    }

    @Test
    void shouldRejectSkippingPublicationPhase() {
        ActivityStagePublicationEntity checking = start();

        assertAll(
                () -> assertThrows(IllegalStateException.class, () -> checking.advance(
                        ActivityStagePublicationPhaseVO.SWITCHING, "operator-001", STARTED_AT.plusMinutes(1))),
                () -> assertThrows(IllegalStateException.class, () -> checking.advance(
                        ActivityStagePublicationPhaseVO.COMPLETED, "operator-001", STARTED_AT.plusMinutes(1)))
        );
    }

    @Test
    void shouldRecordFailureAtCurrentPhaseAndRetrySamePublication() {
        // Given
        ActivityStagePublicationEntity preheating = start().advance(
                ActivityStagePublicationPhaseVO.PREHEATING, "operator-001", STARTED_AT.plusMinutes(1));
        LocalDateTime failedAt = STARTED_AT.plusMinutes(2);
        LocalDateTime retriedAt = STARTED_AT.plusMinutes(3);

        // When
        ActivityStagePublicationEntity failed = preheating.fail(
                "activity.assembly.failed", "活动运行数据预热失败", "operator-001", failedAt);
        ActivityStagePublicationEntity retried = failed.retry("operator-002", retriedAt);

        // Then
        assertAll(
                () -> assertEquals(ActivityStagePublicationStateVO.FAILED, failed.state()),
                () -> assertEquals(ActivityStagePublicationPhaseVO.PREHEATING, failed.phase()),
                () -> assertEquals("activity.assembly.failed", failed.failureCode()),
                () -> assertEquals("活动运行数据预热失败", failed.failureReason()),
                () -> assertEquals(failed.publicationId(), retried.publicationId()),
                () -> assertEquals(ActivityStagePublicationStateVO.PROCESSING, retried.state()),
                () -> assertEquals(ActivityStagePublicationPhaseVO.CHECKING, retried.phase()),
                () -> assertEquals(2, retried.attemptCount()),
                () -> assertNull(retried.failureCode()),
                () -> assertNull(retried.failureReason()),
                () -> assertEquals(retriedAt, retried.updateTime())
        );
    }

    @Test
    void shouldSucceedOnlyFromSwitchingPhase() {
        // Given
        ActivityStagePublicationEntity switching = start()
                .advance(ActivityStagePublicationPhaseVO.PREHEATING, "operator-001", STARTED_AT.plusMinutes(1))
                .advance(ActivityStagePublicationPhaseVO.SWITCHING, "operator-001", STARTED_AT.plusMinutes(2));
        LocalDateTime publishedAt = STARTED_AT.plusMinutes(3);

        // When
        ActivityStagePublicationEntity succeeded = switching.succeed("operator-001", publishedAt);

        // Then
        assertAll(
                () -> assertEquals(ActivityStagePublicationStateVO.SUCCEEDED, succeeded.state()),
                () -> assertEquals(ActivityStagePublicationPhaseVO.COMPLETED, succeeded.phase()),
                () -> assertEquals(publishedAt, succeeded.publishedAt()),
                () -> assertEquals(publishedAt, succeeded.updateTime()),
                () -> assertThrows(IllegalStateException.class,
                        () -> start().succeed("operator-001", publishedAt)),
                () -> assertThrows(IllegalStateException.class,
                        () -> succeeded.retry("operator-001", publishedAt.plusMinutes(1))),
                () -> assertThrows(IllegalStateException.class, () -> succeeded.fail(
                        "unexpected", "不应允许覆盖成功状态", "operator-001", publishedAt.plusMinutes(1)))
        );
    }

    @Test
    void shouldRejectImpossibleStateCombinations() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> publication(
                        ActivityStagePublicationStateVO.PROCESSING, ActivityStagePublicationPhaseVO.COMPLETED,
                        null, null, null)),
                () -> assertThrows(IllegalArgumentException.class, () -> publication(
                        ActivityStagePublicationStateVO.SUCCEEDED, ActivityStagePublicationPhaseVO.COMPLETED,
                        null, null, null)),
                () -> assertThrows(IllegalArgumentException.class, () -> publication(
                        ActivityStagePublicationStateVO.FAILED, ActivityStagePublicationPhaseVO.PREHEATING,
                        null, null, null)),
                () -> assertThrows(IllegalArgumentException.class, () -> publication(
                        ActivityStagePublicationStateVO.PROCESSING, ActivityStagePublicationPhaseVO.CHECKING,
                        "failure", "不应存在失败信息", null)),
                () -> assertThrows(IllegalArgumentException.class, () -> publication(
                        ActivityStagePublicationStateVO.FAILED, ActivityStagePublicationPhaseVO.COMPLETED,
                        "failure", "完成阶段不能失败", null))
        );
    }

    @Test
    void shouldRejectStartingPublicationFromActiveStage() {
        ActivityStageEntity activeStage = stage(ActivityStageStateVO.ACTIVE, 4);

        assertThrows(IllegalArgumentException.class, () -> ActivityStagePublicationEntity.start(
                800001L, activeStage, "operator-001", STARTED_AT));
    }

    private ActivityStagePublicationEntity start() {
        return ActivityStagePublicationEntity.start(800001L, stage(ActivityStageStateVO.CREATE, 3),
                "operator-001", STARTED_AT);
    }

    private ActivityStageEntity stage(ActivityStageStateVO state, int version) {
        return new ActivityStageEntity(900001L, "web", "official", 100301L, state, version,
                "operator-001", "operator-001", STARTED_AT.minusDays(1), STARTED_AT.minusDays(1));
    }

    private ActivityStagePublicationEntity publication(
            ActivityStagePublicationStateVO state,
            ActivityStagePublicationPhaseVO phase,
            String failureCode,
            String failureReason,
            LocalDateTime publishedAt
    ) {
        return new ActivityStagePublicationEntity(800001L, 900001L, 3, 100301L, "web", "official",
                state, phase, 1, "operator-001", failureCode, failureReason, publishedAt, STARTED_AT, STARTED_AT);
    }
}
