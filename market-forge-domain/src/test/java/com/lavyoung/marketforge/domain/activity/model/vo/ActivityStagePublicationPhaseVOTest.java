package com.lavyoung.marketforge.domain.activity.model.vo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 活动展台发布执行阶段测试。
 *
 * <p>验证持久化编码解析和检查、预热、切换、完成之间的顺序约束。</p>
 */
class ActivityStagePublicationPhaseVOTest {

    @Test
    void shouldParsePersistenceCodes() {
        assertAll(
                () -> assertEquals(ActivityStagePublicationPhaseVO.CHECKING,
                        ActivityStagePublicationPhaseVO.of("checking").orElseThrow()),
                () -> assertEquals(ActivityStagePublicationPhaseVO.PREHEATING,
                        ActivityStagePublicationPhaseVO.of("preheating").orElseThrow()),
                () -> assertEquals(ActivityStagePublicationPhaseVO.SWITCHING,
                        ActivityStagePublicationPhaseVO.of("switching").orElseThrow()),
                () -> assertEquals(ActivityStagePublicationPhaseVO.COMPLETED,
                        ActivityStagePublicationPhaseVO.of("completed").orElseThrow()),
                () -> assertTrue(ActivityStagePublicationPhaseVO.of("unknown").isEmpty()),
                () -> assertTrue(ActivityStagePublicationPhaseVO.of(null).isEmpty())
        );
    }

    @Test
    void shouldAllowOnlyDirectPhaseSuccessors() {
        assertAll(
                () -> assertTrue(ActivityStagePublicationPhaseVO.CHECKING
                        .canAdvanceTo(ActivityStagePublicationPhaseVO.PREHEATING)),
                () -> assertTrue(ActivityStagePublicationPhaseVO.PREHEATING
                        .canAdvanceTo(ActivityStagePublicationPhaseVO.SWITCHING)),
                () -> assertTrue(ActivityStagePublicationPhaseVO.SWITCHING
                        .canAdvanceTo(ActivityStagePublicationPhaseVO.COMPLETED)),
                () -> assertFalse(ActivityStagePublicationPhaseVO.CHECKING
                        .canAdvanceTo(ActivityStagePublicationPhaseVO.SWITCHING)),
                () -> assertFalse(ActivityStagePublicationPhaseVO.COMPLETED
                        .canAdvanceTo(ActivityStagePublicationPhaseVO.CHECKING)),
                () -> assertThrows(NullPointerException.class,
                        () -> ActivityStagePublicationPhaseVO.CHECKING.canAdvanceTo(null))
        );
    }
}
