package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStageEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStageStateVO;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityStagePO;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 活动展台持久化转换器测试。
 *
 * <p>验证展台配置双向转换不丢失字段，并确保未知数据库状态无法进入领域层。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
class ActivityStageAssemblerTest {

    private final ActivityStageAssembler assembler = Mappers.getMapper(ActivityStageAssembler.class);

    /**
     * 验证完整展台配置经过 PO 往返转换后保持一致。
     */
    @Test
    void shouldConvertActivityStageInBothDirections() {
        // Given
        ActivityStageEntity expected = stage(ActivityStageStateVO.ACTIVE);

        // When
        ActivityStagePO po = assembler.toPO(expected);
        ActivityStageEntity actual = assembler.toEntity(po);

        // Then
        assertAll(
                () -> assertNull(po.getId()),
                () -> assertEquals("wechat", po.getChannel()),
                () -> assertEquals("campaign", po.getSource()),
                () -> assertEquals("active", po.getState()),
                () -> assertEquals(expected, actual)
        );
    }

    /**
     * 验证数据库未知状态会在基础设施边界立即暴露。
     */
    @Test
    void shouldRejectUnknownStageState() {
        // Given
        ActivityStagePO po = assembler.toPO(stage(ActivityStageStateVO.CREATE));
        po.setState("deleted");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> assembler.toEntity(po));
    }

    private ActivityStageEntity stage(ActivityStageStateVO state) {
        return new ActivityStageEntity(
                900001L,
                "wechat",
                "campaign",
                100001L,
                state,
                2,
                "operator-001",
                "operator-002",
                LocalDateTime.of(2026, 10, 22, 10, 0),
                LocalDateTime.of(2026, 10, 22, 11, 0)
        );
    }
}
