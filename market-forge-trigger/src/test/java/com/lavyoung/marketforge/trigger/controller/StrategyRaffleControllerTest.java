package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.strategy.IStrategyRaffleApi;
import com.lavyoung.marketforge.api.strategy.request.StrategyRaffleRequest;
import com.lavyoung.marketforge.api.strategy.response.StrategyAwardResponse;
import com.lavyoung.marketforge.api.strategy.response.StrategyRaffleResponse;
import com.lavyoung.marketforge.application.strategy.model.RaffleCommand;
import com.lavyoung.marketforge.application.strategy.model.RaffleResult;
import com.lavyoung.marketforge.application.strategy.model.StrategyAwardResult;
import com.lavyoung.marketforge.application.strategy.service.IStrategyRaffleService;
import com.lavyoung.marketforge.trigger.assembler.StrategyAwardAssembler;
import com.lavyoung.marketforge.trigger.assembler.StrategyRaffleAssembler;
import com.lavyoung.marketforge.trigger.exception.GlobalExceptionHandler;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import com.lavyoung.marketforge.types.model.Response;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * 验证抽奖 HTTP 适配器的契约校验、应用命令映射和统一响应包装行为。
 */
class StrategyRaffleControllerTest {

    private static final String USER_ID = "user-001";
    private static final Long STRATEGY_ID = 100_001L;
    private static final Long AWARD_ID = 100_011L;
    private static final StrategyRaffleAssembler STRATEGY_RAFFLE_ASSEMBLER =
            Mappers.getMapper(StrategyRaffleAssembler.class);
    private static final StrategyAwardAssembler STRATEGY_AWARD_ASSEMBLER =
            Mappers.getMapper(StrategyAwardAssembler.class);

    /**
     * Given 合法 API 请求及固定应用结果，When 执行用例，Then 返回隔离后的 API 响应。
     */
    @Test
    void shouldMapApiRequestAndDomainResult() {
        // Given
        IStrategyRaffleService applicationService = mock(IStrategyRaffleService.class);
        RaffleCommand command = new RaffleCommand(USER_ID, STRATEGY_ID);
        when(applicationService.raffle(command)).thenReturn(
                new RaffleResult(STRATEGY_ID, AWARD_ID, "random_ore", "quantity=1", "随机矿石"));
        StrategyRaffleController controller =
                new StrategyRaffleController(
                        applicationService, STRATEGY_RAFFLE_ASSEMBLER, STRATEGY_AWARD_ASSEMBLER);

        // When
        Response<StrategyRaffleResponse> result =
                controller.raffle(new StrategyRaffleRequest(USER_ID, STRATEGY_ID));

        // Then
        assertAll(
                () -> assertEquals(STRATEGY_ID, result.data().strategyId()),
                () -> assertEquals(AWARD_ID, result.data().awardId()),
                () -> assertEquals("random_ore", result.data().awardKey()),
                () -> assertEquals("quantity=1", result.data().awardConfig())
        );
        verify(applicationService).raffle(command);
    }

    /**
     * Given 合法抽奖请求，When 通过 HTTP 入口处理，Then 使用统一成功响应包装业务结果。
     */
    @Test
    void shouldWrapRaffleResultInUnifiedResponse() {
        // Given
        IStrategyRaffleService applicationService = mock(IStrategyRaffleService.class);
        when(applicationService.raffle(org.mockito.ArgumentMatchers.any())).thenReturn(
                new RaffleResult(STRATEGY_ID, AWARD_ID, null, null, null));
        StrategyRaffleController controller =
                new StrategyRaffleController(
                        applicationService, STRATEGY_RAFFLE_ASSEMBLER, STRATEGY_AWARD_ASSEMBLER);

        // When
        Response<StrategyRaffleResponse> result =
                controller.raffle(new StrategyRaffleRequest(USER_ID, STRATEGY_ID));

        // Then
        assertAll(
                () -> assertEquals(CommonResponseCode.SUCCESS.getCode(), result.code()),
                () -> assertNotNull(result.data()),
                () -> assertEquals(AWARD_ID, result.data().awardId())
        );
    }

    /**
     * Given 应用层返回奖品领域实体，When 查询奖品列表，Then 转换并包装为 API 响应列表。
     */
    @Test
    void shouldConvertStrategyAwardListToApiResponse() {
        // Given
        IStrategyRaffleService applicationService = mock(IStrategyRaffleService.class);
        StrategyAwardResult award = new StrategyAwardResult(
                STRATEGY_ID, AWARD_ID, "随机矿石", 100, 80, new BigDecimal("0.1000"), 1);
        when(applicationService.queryRaffleStrategyAwardList(STRATEGY_ID))
                .thenReturn(List.of(award));
        StrategyRaffleController controller = new StrategyRaffleController(
                applicationService, STRATEGY_RAFFLE_ASSEMBLER, STRATEGY_AWARD_ASSEMBLER);

        // When
        Response<List<StrategyAwardResponse>> result =
                controller.queryStrategyAwardList(STRATEGY_ID);

        // Then
        StrategyAwardResponse response = result.data().get(0);
        assertAll(
                () -> assertEquals(CommonResponseCode.SUCCESS.getCode(), result.code()),
                () -> assertEquals(STRATEGY_ID, response.strategyId()),
                () -> assertEquals(AWARD_ID, response.awardId()),
                () -> assertEquals("随机矿石", response.awardTitle()),
                () -> assertEquals(new BigDecimal("0.1000"), response.awardRate())
        );
        verify(applicationService).queryRaffleStrategyAwardList(STRATEGY_ID);
    }

    /**
     * Given 用户标识为空且策略标识非正数，When 执行 Bean Validation，Then 两项约束均被识别。
     */
    @Test
    void shouldRejectStructurallyInvalidRequest() {
        // Given
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = validatorFactory.getValidator();
            StrategyRaffleRequest request = new StrategyRaffleRequest(" ", 0L);

            // When
            var violations = validator.validate(request);

            // Then
            assertEquals(2, violations.size());
        }
    }

    /**
     * Given 空请求，When 直接调用应用端口，Then 快速失败而不是进入领域服务。
     */
    @Test
    void shouldRejectNullRequest() {
        // Given
        StrategyRaffleController controller = new StrategyRaffleController(
                mock(IStrategyRaffleService.class), STRATEGY_RAFFLE_ASSEMBLER, STRATEGY_AWARD_ASSEMBLER);

        // When & Then
        assertThrows(NullPointerException.class, () -> controller.raffle(null));
    }

    /**
     * Given 合法 JSON 请求，When 调用 HTTP 端点，Then 返回 200 与统一成功响应结构。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldExposeRaffleHttpContract() throws Exception {
        // Given
        IStrategyRaffleService applicationService = mock(IStrategyRaffleService.class);
        when(applicationService.raffle(org.mockito.ArgumentMatchers.any())).thenReturn(
                new RaffleResult(STRATEGY_ID, AWARD_ID, "random_ore", null, null));
        MockMvc mockMvc = standaloneSetup(
                new StrategyRaffleController(
                        applicationService, STRATEGY_RAFFLE_ASSEMBLER, STRATEGY_AWARD_ASSEMBLER))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        String requestBody = """
                {
                  "userId": "user-001",
                  "strategyId": 100001
                }
                """;

        // When & Then
        mockMvc.perform(post(IStrategyRaffleApi.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.strategyId").value(STRATEGY_ID))
                .andExpect(jsonPath("$.data.awardId").value(AWARD_ID))
                .andExpect(jsonPath("$.data.awardKey").value("random_ore"));
    }

    /**
     * Given 不符合契约的 JSON 请求，When 调用 HTTP 端点，Then 返回 400 参数错误。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldRejectInvalidHttpRequest() throws Exception {
        // Given
        MockMvc mockMvc = standaloneSetup(new StrategyRaffleController(
                mock(IStrategyRaffleService.class),
                STRATEGY_RAFFLE_ASSEMBLER,
                STRATEGY_AWARD_ASSEMBLER))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        String requestBody = """
                {
                  "userId": " ",
                  "strategyId": 0
                }
                """;

        // When & Then
        mockMvc.perform(post(IStrategyRaffleApi.BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.PARAM_INVALID.getCode()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /**
     * Given 非 JSON 请求，When 调用抽奖端点，Then 按 REST 契约返回不支持的媒体类型。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldRejectUnsupportedMediaType() throws Exception {
        // Given
        MockMvc mockMvc = standaloneSetup(
                new StrategyRaffleController(
                        mock(IStrategyRaffleService.class),
                        STRATEGY_RAFFLE_ASSEMBLER,
                        STRATEGY_AWARD_ASSEMBLER)).build();

        // When & Then
        mockMvc.perform(post(IStrategyRaffleApi.BASE_PATH)
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("user-001,100001"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
