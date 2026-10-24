package com.lavyoung.marketforge.application.award.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link AwardConfigFieldMetadata} 单元测试。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/24
 */
class AwardConfigFieldMetadataTest {

    @Test
    void shouldCreateImmutableFieldMetadata() {
        // Given
        Map<String, String> constraints = new HashMap<>();
        constraints.put("minimum", "1");
        constraints.put("step", "1");

        // When
        AwardConfigFieldMetadata metadata = new AwardConfigFieldMetadata(
                "amount", "积分数量", AwardConfigFieldType.INTEGER, true, "中奖后发放的积分数量", constraints);
        constraints.put("minimum", "0");

        // Then
        assertEquals("amount", metadata.name());
        assertEquals("积分数量", metadata.label());
        assertEquals(AwardConfigFieldType.INTEGER, metadata.fieldType());
        assertEquals(Map.of("minimum", "1", "step", "1"), metadata.constraints());
        assertThrows(UnsupportedOperationException.class, () -> metadata.constraints().put("maximum", "100"));
    }

    @Test
    void shouldRejectBlankRequiredText() {
        // Given
        Map<String, String> constraints = Map.of();

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> new AwardConfigFieldMetadata(
                " ", "积分数量", AwardConfigFieldType.INTEGER, true, "中奖后发放的积分数量", constraints));
        assertThrows(IllegalArgumentException.class, () -> new AwardConfigFieldMetadata(
                "amount", " ", AwardConfigFieldType.INTEGER, true, "中奖后发放的积分数量", constraints));
        assertThrows(IllegalArgumentException.class, () -> new AwardConfigFieldMetadata(
                "amount", "积分数量", AwardConfigFieldType.INTEGER, true, " ", constraints));
    }

    @Test
    void shouldRejectNullFieldTypeAndConstraints() {
        // Given & When & Then
        assertThrows(NullPointerException.class, () -> new AwardConfigFieldMetadata(
                "amount", "积分数量", null, true, "中奖后发放的积分数量", Map.of()));
        assertThrows(NullPointerException.class, () -> new AwardConfigFieldMetadata(
                "amount", "积分数量", AwardConfigFieldType.INTEGER, true, "中奖后发放的积分数量", null));
    }
}
