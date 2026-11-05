package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountDefinitionEntity;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityCountPO;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 活动次数配置运营定义转换测试。
 *
 * <p>验证单对象和集合转换复用同一字段规则，不遗漏版本及审计信息。</p>
 */
class ActivityCountDefinitionAssemblerTest {

    private final ActivityCountDefinitionAssembler assembler = Mappers.getMapper(ActivityCountDefinitionAssembler.class);

    @Test
    void shouldConvertDefinitionInBothDirections() {
        // Given
        ActivityCountDefinitionEntity expected = definition(100301L, "基础次数包");

        // When
        ActivityCountPO po = assembler.toPO(expected);
        ActivityCountDefinitionEntity actual = assembler.toEntity(po);

        // Then
        assertNull(po.getId());
        assertEquals(expected, actual);
    }

    @Test
    void shouldConvertDefinitionList() {
        // Given
        ActivityCountDefinitionEntity first = definition(100301L, "基础次数包");
        ActivityCountDefinitionEntity second = definition(100302L, "高级次数包");
        List<ActivityCountPO> poList = List.of(assembler.toPO(first), assembler.toPO(second));

        // When
        List<ActivityCountDefinitionEntity> actual = assembler.toEntities(poList);

        // Then
        assertEquals(List.of(first, second), actual);
    }

    private ActivityCountDefinitionEntity definition(Long activityCountId, String activityCountName) {
        return new ActivityCountDefinitionEntity(activityCountId, activityCountName, 100, 10, 50, 1,
                "admin", "operator", LocalDateTime.of(2026, 11, 4, 10, 0),
                LocalDateTime.of(2026, 11, 4, 11, 0));
    }
}
