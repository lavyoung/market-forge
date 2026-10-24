package com.lavyoung.marketforge.application.award.service;

import com.lavyoung.marketforge.application.award.metadata.AwardTypeMetadataRegistry;
import com.lavyoung.marketforge.application.award.model.AwardClaimModeMetadata;
import com.lavyoung.marketforge.application.award.model.AwardTypeMetadata;
import com.lavyoung.marketforge.application.award.service.impl.AwardTypeMetadataApplicationServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

/**
 * {@link AwardTypeMetadataApplicationServiceImpl} 单元测试。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@ExtendWith(MockitoExtension.class)
class AwardTypeMetadataApplicationServiceImplTest {

    @Mock
    private AwardTypeMetadataRegistry metadataRegistry;

    @Test
    void shouldReturnValidatedMetadataFromRegistry() {
        // Given
        AwardTypeMetadata metadata = new AwardTypeMetadata(
                "internal_credit", "平台积分",
                List.of(new AwardClaimModeMetadata("automatic", "自动发放")),
                1, List.of(), "自动发放平台积分");
        List<AwardTypeMetadata> expected = List.of(metadata);
        when(metadataRegistry.listAll()).thenReturn(expected);
        IAwardTypeMetadataApplicationService service = new AwardTypeMetadataApplicationServiceImpl(metadataRegistry);

        // When
        List<AwardTypeMetadata> result = service.listSupportedTypes();

        // Then
        assertSame(expected, result);
        assertEquals(List.of("internal_credit"), result.stream().map(AwardTypeMetadata::awardType).toList());
        verify(metadataRegistry).listAll();
        verifyNoMoreInteractions(metadataRegistry);
    }
}
