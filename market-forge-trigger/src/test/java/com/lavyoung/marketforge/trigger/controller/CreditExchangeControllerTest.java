package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.credit.ICreditExchangeApi;
import com.lavyoung.marketforge.api.credit.request.CreditExchangeRequest;
import com.lavyoung.marketforge.application.credit.model.ExchangeCreditForSkuCommand;
import com.lavyoung.marketforge.application.credit.service.ICreditExchangeApplicationService;
import com.lavyoung.marketforge.trigger.assembler.CreditExchangeRequestAssembler;
import com.lavyoung.marketforge.trigger.exception.GlobalExceptionHandler;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import com.lavyoung.marketforge.types.model.Response;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * 验证积分兑换 HTTP 适配器的命令转换、统一响应包装和参数校验。
 */
class CreditExchangeControllerTest {

    private static final String USER_ID = "user-001";
    private static final Long SKU = 9011L;
    private static final Long CREDIT_AMOUNT = 100L;
    private static final String OUT_BUSINESS_NO = "exchange-user-001-20261011-001";
    private static final String RECHARGE_ORDER_ID = "activity-order-001";
    private static final CreditExchangeRequestAssembler ASSEMBLER =
            Mappers.getMapper(CreditExchangeRequestAssembler.class);

    /**
     * Given 合法积分兑换请求，When 直接调用 Controller，Then 应转换应用命令并包装活动订单号。
     */
    @Test
    void shouldMapApiRequestAndWrapApplicationResult() {
        // Given
        ICreditExchangeApplicationService applicationService =
                mock(ICreditExchangeApplicationService.class);
        ExchangeCreditForSkuCommand expectedCommand =
                new ExchangeCreditForSkuCommand(USER_ID, SKU, CREDIT_AMOUNT, OUT_BUSINESS_NO);
        when(applicationService.exchangeCreditForSku(expectedCommand)).thenReturn(RECHARGE_ORDER_ID);
        CreditExchangeController controller =
                new CreditExchangeController(applicationService, ASSEMBLER);

        // When
        Response<String> result = controller.exchangeCreditForSku(request());

        // Then
        assertAll(
                () -> assertEquals(CommonResponseCode.SUCCESS.getCode(), result.code()),
                () -> assertEquals(RECHARGE_ORDER_ID, result.data())
        );
        verify(applicationService).exchangeCreditForSku(expectedCommand);
    }

    /**
     * Given 结构非法的积分兑换请求，When 执行 Bean Validation，Then 必填和正数约束应生效。
     */
    @Test
    void shouldRejectStructurallyInvalidRequest() {
        // Given
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = validatorFactory.getValidator();
            CreditExchangeRequest request = new CreditExchangeRequest(" ", null, 0L, " ");

            // When
            var violations = validator.validate(request);

            // Then
            assertEquals(4, violations.size());
        }
    }

    /**
     * Given 合法 JSON 请求，When 调用积分兑换 HTTP 端点，Then 返回统一成功响应。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldExposeCreditExchangeHttpContract() throws Exception {
        // Given
        ICreditExchangeApplicationService applicationService =
                mock(ICreditExchangeApplicationService.class);
        when(applicationService.exchangeCreditForSku(new ExchangeCreditForSkuCommand(
                USER_ID, SKU, CREDIT_AMOUNT, OUT_BUSINESS_NO))).thenReturn(RECHARGE_ORDER_ID);
        MockMvc mockMvc = standaloneSetup(new CreditExchangeController(applicationService, ASSEMBLER))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        String requestBody = """
                {
                  "userId": "user-001",
                  "sku": 9011,
                  "creditAmount": 100,
                  "outBusinessNo": "exchange-user-001-20261011-001"
                }
                """;

        // When & Then
        mockMvc.perform(post(ICreditExchangeApi.BASE_PATH + "/sku")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data").value(RECHARGE_ORDER_ID));

        verify(applicationService).exchangeCreditForSku(
                new ExchangeCreditForSkuCommand(USER_ID, SKU, CREDIT_AMOUNT, OUT_BUSINESS_NO));
    }

    /**
     * Given 不符合契约的 JSON 请求，When 调用积分兑换 HTTP 端点，Then 返回 400 且不会进入应用服务。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldRejectInvalidHttpRequest() throws Exception {
        // Given
        ICreditExchangeApplicationService applicationService =
                mock(ICreditExchangeApplicationService.class);
        MockMvc mockMvc = standaloneSetup(new CreditExchangeController(applicationService, ASSEMBLER))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        String requestBody = """
                {
                  "userId": " ",
                  "sku": null,
                  "creditAmount": 0,
                  "outBusinessNo": " "
                }
                """;

        // When & Then
        mockMvc.perform(post(ICreditExchangeApi.BASE_PATH + "/sku")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.PARAM_INVALID.getCode()))
                .andExpect(jsonPath("$.data").doesNotExist());

        verifyNoInteractions(applicationService);
    }

    private static CreditExchangeRequest request() {
        return new CreditExchangeRequest(USER_ID, SKU, CREDIT_AMOUNT, OUT_BUSINESS_NO);
    }
}
