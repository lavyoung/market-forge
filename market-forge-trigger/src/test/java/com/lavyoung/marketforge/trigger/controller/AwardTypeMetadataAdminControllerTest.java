package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.award.IAwardTypeMetadataAdminApi;
import com.lavyoung.marketforge.api.award.response.AwardClaimModeMetadataResponse;
import com.lavyoung.marketforge.api.award.response.AwardConfigFieldMetadataResponse;
import com.lavyoung.marketforge.api.award.response.AwardTypeMetadataResponse;
import com.lavyoung.marketforge.application.award.model.AwardClaimModeMetadata;
import com.lavyoung.marketforge.application.award.model.AwardTypeMetadata;
import com.lavyoung.marketforge.application.award.service.IAwardTypeMetadataApplicationService;
import com.lavyoung.marketforge.trigger.assembler.AwardTypeMetadataApiAssembler;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * {@link AwardTypeMetadataAdminController} HTTP 契约测试。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@ExtendWith(MockitoExtension.class)
class AwardTypeMetadataAdminControllerTest {

    @Mock
    private IAwardTypeMetadataApplicationService applicationService;

    @Mock
    private AwardTypeMetadataApiAssembler apiAssembler;

    @Test
    void shouldExposeSupportedAwardTypeMetadata() throws Exception {
        // Given
        AwardTypeMetadata metadata = new AwardTypeMetadata(
                "internal_credit", "平台积分",
                List.of(new AwardClaimModeMetadata("automatic", "自动发放")),
                1, List.of(), "中奖后自动发放积分");
        AwardTypeMetadataResponse response = new AwardTypeMetadataResponse(
                "internal_credit", "平台积分",
                List.of(new AwardClaimModeMetadataResponse("automatic", "自动发放")), 1,
                List.of(new AwardConfigFieldMetadataResponse("amount", "积分数量", "integer", true,
                        "中奖后发放的积分数量", Map.of("minimum", "1"))),
                "中奖后自动发放积分");
        when(applicationService.listSupportedTypes()).thenReturn(List.of(metadata));
        when(apiAssembler.toResponses(List.of(metadata))).thenReturn(List.of(response));
        MockMvc mockMvc = standaloneSetup(new AwardTypeMetadataAdminController(applicationService, apiAssembler)).build();

        // When & Then
        mockMvc.perform(get(IAwardTypeMetadataAdminApi.BASE_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data[0].awardType").value("internal_credit"))
                .andExpect(jsonPath("$.data[0].supportedClaimModes[0].code").value("automatic"))
                .andExpect(jsonPath("$.data[0].supportedClaimModes[0].displayName").value("自动发放"))
                .andExpect(jsonPath("$.data[0].configFields[0].fieldType").value("integer"))
                .andExpect(jsonPath("$.data[0].configFields[0].constraints.minimum").value("1"));

        verify(applicationService).listSupportedTypes();
        verify(apiAssembler).toResponses(List.of(metadata));
    }
}
