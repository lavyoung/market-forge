package com.lavyoung.marketforge.domain.activity.model.vo;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 活动展台生命周期状态单元测试。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
class ActivityStageStateVOTest {

    /**
     * 验证持久化状态编码可以安全解析。
     */
    @Test
    void shouldResolveStateByCode() {
        // Given & When
        Optional<ActivityStageStateVO> active = ActivityStageStateVO.of("active");

        // Then
        assertAll(
                () -> assertEquals(Optional.of(ActivityStageStateVO.ACTIVE), active),
                () -> assertTrue(ActivityStageStateVO.of("unknown").isEmpty()),
                () -> assertTrue(ActivityStageStateVO.of(null).isEmpty())
        );
    }

    /**
     * 验证展台状态只能按照发布生命周期单向推进。
     */
    @Test
    void shouldAllowOnlyForwardLifecycleTransitions() {
        // Given & When & Then
        assertAll(
                () -> assertTrue(ActivityStageStateVO.CREATE.canTransitionTo(ActivityStageStateVO.ACTIVE)),
                () -> assertTrue(ActivityStageStateVO.ACTIVE.canTransitionTo(ActivityStageStateVO.EXPIRED)),
                () -> assertFalse(ActivityStageStateVO.CREATE.canTransitionTo(ActivityStageStateVO.EXPIRED)),
                () -> assertFalse(ActivityStageStateVO.ACTIVE.canTransitionTo(ActivityStageStateVO.CREATE)),
                () -> assertFalse(ActivityStageStateVO.EXPIRED.canTransitionTo(ActivityStageStateVO.ACTIVE)),
                () -> assertThrows(NullPointerException.class, () -> ActivityStageStateVO.CREATE.canTransitionTo(null))
        );
    }
}
