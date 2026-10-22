package com.lavyoung.marketforge.domain.activity.model.entity;

import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStageStateVO;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 活动展台领域实体单元测试。
 *
 * <p>覆盖合法配置、生命周期能力以及关键输入边界，确保非法渠道编码和无效业务标识
 * 在进入持久化层之前就被领域模型拒绝。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
class ActivityStageEntityTest {

    /**
     * 验证待上架实体可以编辑但尚未对外生效。
     */
    @Test
    void shouldCreateEditableStage() {
        // Given
        ActivityStageEntity stage = createStage(ActivityStageStateVO.CREATE);

        // When & Then
        assertAll(
                () -> assertTrue(stage.canEdit()),
                () -> assertFalse(stage.isActive())
        );
    }

    /**
     * 验证已生效实体可以被用户端查询但不允许继续编辑。
     */
    @Test
    void shouldRecognizeActiveStage() {
        // Given
        ActivityStageEntity stage = createStage(ActivityStageStateVO.ACTIVE);

        // When & Then
        assertAll(
                () -> assertFalse(stage.canEdit()),
                () -> assertTrue(stage.isActive())
        );
    }

    /**
     * 验证非法渠道、活动标识和版本会被拒绝。
     */
    @Test
    void shouldRejectInvalidStageConfiguration() {
        // Given & When & Then
        assertAll(
                () -> assertThrows(IllegalArgumentException.class, () -> createStage("Wechat", 100001L, 1)),
                () -> assertThrows(IllegalArgumentException.class, () -> createStage("wechat", 0L, 1)),
                () -> assertThrows(IllegalArgumentException.class, () -> createStage("wechat", 100001L, 0))
        );
    }

    /**
     * 验证状态变更同时约束生命周期、乐观锁版本和操作人。
     */
    @Test
    void shouldValidateStageStateChange() {
        // Given & When
        ActivityStageStateChange validChange = new ActivityStageStateChange(
                900001L, ActivityStageStateVO.CREATE, ActivityStageStateVO.ACTIVE, 1, "admin");

        // Then
        assertAll(
                () -> assertTrue(validChange.currentState().canTransitionTo(validChange.targetState())),
                () -> assertThrows(IllegalArgumentException.class, () -> new ActivityStageStateChange(
                        900001L, ActivityStageStateVO.EXPIRED, ActivityStageStateVO.ACTIVE, 1, "admin")),
                () -> assertThrows(IllegalArgumentException.class, () -> new ActivityStageStateChange(
                        900001L, ActivityStageStateVO.CREATE, ActivityStageStateVO.ACTIVE, 0, "admin"))
        );
    }

    private ActivityStageEntity createStage(ActivityStageStateVO state) {
        return new ActivityStageEntity(900001L, "wechat", "campaign", 100001L, state, 1, "admin", "admin", null, null);
    }

    private ActivityStageEntity createStage(String channel, Long activityId, int version) {
        return new ActivityStageEntity(900001L, channel, "campaign", activityId, ActivityStageStateVO.CREATE, version, "admin", "admin", null, null);
    }
}
