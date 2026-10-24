package com.lavyoung.marketforge.application.award.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link AwardTypeMetadata} 单元测试。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/24
 */
class AwardTypeMetadataTest {

    @Test
    void shouldCreateImmutableAwardTypeMetadata() {
        // Given
        List<AwardClaimModeMetadata> claimModes = new ArrayList<>(List.of(automaticClaimMode()));
        List<AwardConfigFieldMetadata> fields = new ArrayList<>(List.of(amountField()));

        // When
        AwardTypeMetadata metadata = new AwardTypeMetadata(
                "internal_credit", "平台积分", claimModes, 1, fields, "自动发放平台积分");
        claimModes.add(new AwardClaimModeMetadata("manual", "人工领取"));
        fields.clear();

        // Then
        assertEquals(List.of(automaticClaimMode()), metadata.supportedClaimModes());
        assertEquals(List.of(amountField()), metadata.configFields());
        assertThrows(UnsupportedOperationException.class,
                () -> metadata.supportedClaimModes().add(new AwardClaimModeMetadata("manual", "人工领取")));
        assertThrows(UnsupportedOperationException.class, () -> metadata.configFields().clear());
    }

    @Test
    void shouldRejectInvalidBasicMetadata() {
        // Given
        List<AwardClaimModeMetadata> claimModes = List.of(automaticClaimMode());

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> new AwardTypeMetadata(
                " ", "平台积分", claimModes, 1, List.of(), "自动发放平台积分"));
        assertThrows(IllegalArgumentException.class, () -> new AwardTypeMetadata(
                "internal_credit", " ", claimModes, 1, List.of(), "自动发放平台积分"));
        assertThrows(IllegalArgumentException.class, () -> new AwardTypeMetadata(
                "internal_credit", "平台积分", claimModes, 0, List.of(), "自动发放平台积分"));
        assertThrows(IllegalArgumentException.class, () -> new AwardTypeMetadata(
                "internal_credit", "平台积分", claimModes, 1, List.of(), " "));
    }

    @Test
    void shouldRejectInvalidClaimModesAndCollections() {
        // Given & When & Then
        assertThrows(NullPointerException.class, () -> new AwardTypeMetadata(
                "internal_credit", "平台积分", null, 1, List.of(), "自动发放平台积分"));
        assertThrows(NullPointerException.class, () -> new AwardTypeMetadata(
                "internal_credit", "平台积分", List.of(automaticClaimMode()), 1, null, "自动发放平台积分"));
        assertThrows(IllegalArgumentException.class, () -> new AwardTypeMetadata(
                "internal_credit", "平台积分", List.of(), 1, List.of(), "自动发放平台积分"));
        assertThrows(IllegalArgumentException.class, () -> new AwardTypeMetadata(
                "internal_credit", "平台积分",
                List.of(automaticClaimMode(), new AwardClaimModeMetadata("automatic", "自动到账")),
                1, List.of(), "自动发放平台积分"));
        assertThrows(NullPointerException.class, () -> new AwardTypeMetadata(
                "internal_credit", "平台积分", java.util.Arrays.asList(automaticClaimMode(), null),
                1, List.of(), "自动发放平台积分"));
    }

    private AwardConfigFieldMetadata amountField() {
        return new AwardConfigFieldMetadata("amount", "积分数量", AwardConfigFieldType.INTEGER, true,
                "中奖后发放的积分数量", Map.of("minimum", "1", "step", "1"));
    }

    private AwardClaimModeMetadata automaticClaimMode() {
        return new AwardClaimModeMetadata("automatic", "自动发放");
    }
}
