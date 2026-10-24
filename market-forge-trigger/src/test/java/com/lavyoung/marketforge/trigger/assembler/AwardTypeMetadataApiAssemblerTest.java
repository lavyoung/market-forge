package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.award.response.AwardTypeMetadataResponse;
import com.lavyoung.marketforge.application.award.model.AwardClaimModeMetadata;
import com.lavyoung.marketforge.application.award.model.AwardConfigFieldMetadata;
import com.lavyoung.marketforge.application.award.model.AwardConfigFieldType;
import com.lavyoung.marketforge.application.award.model.AwardTypeMetadata;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link AwardTypeMetadataApiAssembler} 单元测试。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
class AwardTypeMetadataApiAssemblerTest {

    private static final AwardTypeMetadataApiAssembler ASSEMBLER =
            Mappers.getMapper(AwardTypeMetadataApiAssembler.class);

    @Test
    void shouldMapCompleteAwardTypeMetadata() {
        // Given
        AwardTypeMetadata metadata = new AwardTypeMetadata(
                "internal_credit", "平台积分",
                List.of(new AwardClaimModeMetadata("automatic", "自动发放")), 1,
                List.of(new AwardConfigFieldMetadata("amount", "积分数量", AwardConfigFieldType.INTEGER,
                        true, "中奖后发放的积分数量", Map.of("minimum", "1", "step", "1"))),
                "中奖后自动发放到平台积分账户");

        // When
        AwardTypeMetadataResponse response = ASSEMBLER.toResponse(metadata);

        // Then
        assertAll(
                () -> assertEquals("internal_credit", response.awardType()),
                () -> assertEquals("平台积分", response.displayName()),
                () -> assertEquals("automatic", response.supportedClaimModes().get(0).code()),
                () -> assertEquals("自动发放", response.supportedClaimModes().get(0).displayName()),
                () -> assertEquals("integer", response.configFields().get(0).fieldType()),
                () -> assertEquals(Map.of("minimum", "1", "step", "1"),
                        response.configFields().get(0).constraints()),
                () -> assertEquals(1, response.configVersion())
        );
        assertThrows(UnsupportedOperationException.class, () -> response.configFields().clear());
    }

    @Test
    void shouldMapMetadataListInOriginalOrder() {
        // Given
        AwardTypeMetadata metadata = new AwardTypeMetadata(
                "internal_credit", "平台积分",
                List.of(new AwardClaimModeMetadata("automatic", "自动发放")),
                1, List.of(), "中奖后自动发放积分");

        // When
        List<AwardTypeMetadataResponse> responses = ASSEMBLER.toResponses(List.of(metadata));

        // Then
        assertEquals(List.of("internal_credit"), responses.stream().map(AwardTypeMetadataResponse::awardType).toList());
    }
}
