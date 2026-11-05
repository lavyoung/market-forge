package com.lavyoung.marketforge.domain.activity.model.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 活动展台发布任务状态测试。
 *
 * <p>验证持久化编码解析以及处理、成功、失败之间的合法迁移关系。</p>
 */
class ActivityStagePublicationStateVOTest {

    @Test
    void shouldParsePersistenceCodes() {
        assertAll(
                () -> assertEquals(ActivityStagePublicationStateVO.PROCESSING,
                        ActivityStagePublicationStateVO.of("processing").orElseThrow()),
                () -> assertEquals(ActivityStagePublicationStateVO.SUCCEEDED,
                        ActivityStagePublicationStateVO.of("succeeded").orElseThrow()),
                () -> assertEquals(ActivityStagePublicationStateVO.FAILED,
                        ActivityStagePublicationStateVO.of("failed").orElseThrow()),
                () -> assertTrue(ActivityStagePublicationStateVO.of("unknown").isEmpty()),
                () -> assertTrue(ActivityStagePublicationStateVO.of(null).isEmpty())
        );
    }

    @Test
    void shouldAllowOnlyDefinedStateTransitions() {
        assertAll(
                () -> assertTrue(ActivityStagePublicationStateVO.PROCESSING
                        .canTransitionTo(ActivityStagePublicationStateVO.SUCCEEDED)),
                () -> assertTrue(ActivityStagePublicationStateVO.PROCESSING
                        .canTransitionTo(ActivityStagePublicationStateVO.FAILED)),
                () -> assertTrue(ActivityStagePublicationStateVO.FAILED
                        .canTransitionTo(ActivityStagePublicationStateVO.PROCESSING)),
                () -> assertFalse(ActivityStagePublicationStateVO.FAILED
                        .canTransitionTo(ActivityStagePublicationStateVO.SUCCEEDED)),
                () -> assertFalse(ActivityStagePublicationStateVO.SUCCEEDED
                        .canTransitionTo(ActivityStagePublicationStateVO.PROCESSING)),
                () -> assertThrows(NullPointerException.class,
                        () -> ActivityStagePublicationStateVO.PROCESSING.canTransitionTo(null))
        );
    }
}
