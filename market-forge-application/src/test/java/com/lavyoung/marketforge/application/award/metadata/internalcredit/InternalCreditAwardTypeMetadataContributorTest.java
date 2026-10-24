package com.lavyoung.marketforge.application.award.metadata.internalcredit;

import com.lavyoung.marketforge.application.award.model.AwardConfigFieldMetadata;
import com.lavyoung.marketforge.application.award.model.AwardConfigFieldType;
import com.lavyoung.marketforge.application.award.model.AwardTypeMetadata;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link InternalCreditAwardTypeMetadataContributor} 单元测试。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/24
 */
class InternalCreditAwardTypeMetadataContributorTest {

    @Test
    void shouldContributeSupportedInternalCreditMetadata() {
        // Given
        InternalCreditAwardTypeMetadataContributor contributor = new InternalCreditAwardTypeMetadataContributor();

        // When
        AwardTypeMetadata metadata = contributor.metadata();

        // Then
        assertEquals("internal_credit", metadata.awardType());
        assertEquals("平台积分", metadata.displayName());
        assertEquals(List.of("automatic"), metadata.supportedClaimModes());
        assertEquals(1, metadata.configVersion());
        assertEquals(List.of(new AwardConfigFieldMetadata("amount", "积分数量", AwardConfigFieldType.INTEGER,
                        true, "中奖后自动发放到用户积分账户的积分数量", Map.of("minimum", "1", "step", "1"))),
                metadata.configFields());
    }

    @Test
    void shouldReturnSharedImmutableMetadata() {
        // Given
        InternalCreditAwardTypeMetadataContributor contributor = new InternalCreditAwardTypeMetadataContributor();

        // When & Then
        assertSame(contributor.metadata(), contributor.metadata());
    }
}
