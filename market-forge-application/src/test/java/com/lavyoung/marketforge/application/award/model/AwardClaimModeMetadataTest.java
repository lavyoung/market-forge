package com.lavyoung.marketforge.application.award.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link AwardClaimModeMetadata} 单元测试。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
class AwardClaimModeMetadataTest {

    @Test
    void shouldCreateClaimModeMetadata() {
        // Given & When
        AwardClaimModeMetadata metadata = new AwardClaimModeMetadata("automatic", "自动发放");

        // Then
        assertEquals("automatic", metadata.code());
        assertEquals("自动发放", metadata.displayName());
    }

    @Test
    void shouldRejectBlankCodeAndDisplayName() {
        // Given & When & Then
        assertThrows(IllegalArgumentException.class, () -> new AwardClaimModeMetadata(" ", "自动发放"));
        assertThrows(IllegalArgumentException.class, () -> new AwardClaimModeMetadata("automatic", " "));
    }
}
