package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStagePublicationEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStagePublicationPhaseVO;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStagePublicationStateVO;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityStagePublicationPO;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 活动展台发布记录持久化转换器测试。
 *
 * <p>验证发布记录双向转换不会丢失状态、阶段、失败信息和时间字段，
 * 同时确保数据库中的未知编码不能进入领域层。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/06
 */
class ActivityStagePublicationAssemblerTest {

    private final ActivityStagePublicationAssembler assembler =
            Mappers.getMapper(ActivityStagePublicationAssembler.class);

    /**
     * 验证完整发布记录经过 PO 往返转换后保持不变。
     */
    @Test
    void shouldConvertPublicationInBothDirections() {
        // Given
        ActivityStagePublicationEntity expected = publication();

        // When
        ActivityStagePublicationPO po = assembler.toPO(expected);
        ActivityStagePublicationEntity actual = assembler.toEntity(po);

        // Then
        assertAll(
                () -> assertNull(po.getId()),
                () -> assertEquals("processing", po.getState()),
                () -> assertEquals("preheating", po.getPhase()),
                () -> assertEquals(expected, actual)
        );
    }

    /**
     * 验证未知发布状态会在基础设施边界立即暴露。
     */
    @Test
    void shouldRejectUnknownPublicationState() {
        // Given
        ActivityStagePublicationPO po = assembler.toPO(publication());
        po.setState("unknown");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> assembler.toEntity(po));
    }

    /**
     * 验证未知发布阶段会在基础设施边界立即暴露。
     */
    @Test
    void shouldRejectUnknownPublicationPhase() {
        // Given
        ActivityStagePublicationPO po = assembler.toPO(publication());
        po.setPhase("unknown");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> assembler.toEntity(po));
    }

    private ActivityStagePublicationEntity publication() {
        LocalDateTime createTime = LocalDateTime.of(2026, 11, 6, 10, 0);
        return new ActivityStagePublicationEntity(
                910001L, 900001L, 3, 100001L, "wechat", "campaign",
                ActivityStagePublicationStateVO.PROCESSING, ActivityStagePublicationPhaseVO.PREHEATING,
                2, "operator-001", null, null, null, createTime, createTime.plusMinutes(1));
    }
}
