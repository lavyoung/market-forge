package com.lavyoung.marketforge.domain.activity.model.entity;

import com.lavyoung.marketforge.domain.activity.model.vo.ActivityDefinitionStateVO;
import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 活动定义领域规则测试。
 */
class ActivityDefinitionEntityTest {
    @Test
    void shouldAcceptValidDraftAndAllowEditing() {
        // Given & When
        ActivityDefinitionEntity definition = definition(ActivityDefinitionStateVO.DRAFT,
                LocalDateTime.of(2026, 11, 1, 0, 0), LocalDateTime.of(2026, 11, 30, 23, 59));

        // Then
        assertTrue(definition.canEdit());
    }

    @Test
    void shouldRejectInvalidTimeWindow() {
        // Given
        LocalDateTime time = LocalDateTime.of(2026, 11, 1, 0, 0);

        // When & Then
        assertThrows(IllegalArgumentException.class,
                () -> definition(ActivityDefinitionStateVO.DRAFT, time, time));
    }

    @Test
    void shouldOnlyAllowForwardLifecycleTransitions() {
        // When & Then
        assertAll(
                () -> assertTrue(ActivityDefinitionStateVO.DRAFT.canTransitionTo(ActivityDefinitionStateVO.OPEN)),
                () -> assertTrue(ActivityDefinitionStateVO.OPEN.canTransitionTo(ActivityDefinitionStateVO.CLOSE)),
                () -> assertFalse(ActivityDefinitionStateVO.OPEN.canTransitionTo(ActivityDefinitionStateVO.DRAFT)),
                () -> assertFalse(ActivityDefinitionStateVO.CLOSE.canTransitionTo(ActivityDefinitionStateVO.OPEN)));
    }

    private ActivityDefinitionEntity definition(ActivityDefinitionStateVO state, LocalDateTime begin, LocalDateTime end) {
        return new ActivityDefinitionEntity(100301L, "运营活动", "运营活动说明", begin, end, 100301L,
                100001L, AwardUnlockScopeVO.TOTAL, state, 1, "admin", "admin", null, null);
    }
}
