package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.behavior.IBehaviorRebateApi;
import com.lavyoung.marketforge.api.behavior.request.BehaviorRebateRequest;
import com.lavyoung.marketforge.api.behavior.response.BehaviorRebateResponse;
import com.lavyoung.marketforge.application.behavior.model.BehaviorRebateOrderCreateResult;
import com.lavyoung.marketforge.application.behavior.model.CreateBehaviorRebateOrderCommand;
import com.lavyoung.marketforge.application.behavior.service.IBehaviorRebateApplicationService;
import com.lavyoung.marketforge.trigger.assembler.BehaviorRebateResponseAssembler;
import com.lavyoung.marketforge.trigger.exception.GlobalExceptionHandler;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import com.lavyoung.marketforge.types.model.Response;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * 验证用户行为返利 HTTP 适配器的参数校验、命令转换和统一响应包装。
 */
class BehaviorRebateControllerTest {

    private static final String USER_ID = "user-001";
    private static final String OUT_BUSINESS_NO = "sign-20261010-user-001";
    private static final String ORDER_ID = "RO202610100001";
    private static final String EVENT_ID = "event-001";
    private static final BehaviorRebateResponseAssembler ASSEMBLER =
            Mappers.getMapper(BehaviorRebateResponseAssembler.class);

    /**
     * Given 合法 API 请求和固定应用结果，When 直接调用 Controller，Then 应转换命令并包装响应。
     */
    @Test
    void shouldMapApiRequestAndWrapApplicationResult() {
        // Given
        IBehaviorRebateApplicationService applicationService =
                mock(IBehaviorRebateApplicationService.class);
        CreateBehaviorRebateOrderCommand expectedCommand =
                new CreateBehaviorRebateOrderCommand(USER_ID, "sign", OUT_BUSINESS_NO);
        when(applicationService.createOrder(expectedCommand)).thenReturn(
                new BehaviorRebateOrderCreateResult(List.of(ORDER_ID), List.of(EVENT_ID))
        );
        BehaviorRebateController controller =
                new BehaviorRebateController(applicationService, ASSEMBLER);

        // When
        Response<BehaviorRebateResponse> result =
                controller.createOrder(new BehaviorRebateRequest(USER_ID, "sign", OUT_BUSINESS_NO));

        // Then
        assertAll(
                () -> assertEquals(CommonResponseCode.SUCCESS.getCode(), result.code()),
                () -> assertEquals(List.of(ORDER_ID), result.data().orderIds()),
                () -> assertEquals(List.of(EVENT_ID), result.data().eventIds())
        );
        verify(applicationService).createOrder(expectedCommand);
    }

    /**
     * Given 必填字段为空白，When 执行 Bean Validation，Then 三个字段约束都应生效。
     */
    @Test
    void shouldRejectStructurallyInvalidRequest() {
        // Given
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = validatorFactory.getValidator();
            BehaviorRebateRequest request = new BehaviorRebateRequest(" ", " ", " ");

            // When
            var violations = validator.validate(request);

            // Then
            assertEquals(3, violations.size());
        }
    }

    /**
     * Given 合法 JSON 请求，When 调用 HTTP 端点，Then 返回统一成功响应。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldExposeBehaviorRebateHttpContract() throws Exception {
        // Given
        IBehaviorRebateApplicationService applicationService =
                mock(IBehaviorRebateApplicationService.class);
        when(applicationService.createOrder(org.mockito.ArgumentMatchers.any())).thenReturn(
                new BehaviorRebateOrderCreateResult(List.of(ORDER_ID), List.of(EVENT_ID))
        );
        MockMvc mockMvc = standaloneSetup(new BehaviorRebateController(applicationService, ASSEMBLER))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        String requestBody = """
                {
                  "userId": "user-001",
                  "behaviorType": "sign",
                  "outBusinessNo": "sign-20261010-user-001"
                }
                """;

        // When & Then
        mockMvc.perform(post(IBehaviorRebateApi.BASE_PATH + "/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.orderIds[0]").value(ORDER_ID))
                .andExpect(jsonPath("$.data.eventIds[0]").value(EVENT_ID));
    }

    /**
     * Given 签到用户标识，When 直接调用日历签到接口，Then 应自动生成签到行为命令和当天幂等号。
     */
    @Test
    void shouldCreateCalendarSignRebateCommand() {
        // Given
        IBehaviorRebateApplicationService applicationService =
                mock(IBehaviorRebateApplicationService.class);
        when(applicationService.createOrder(org.mockito.ArgumentMatchers.any())).thenReturn(
                new BehaviorRebateOrderCreateResult(List.of(ORDER_ID), List.of(EVENT_ID))
        );
        BehaviorRebateController controller =
                new BehaviorRebateController(applicationService, ASSEMBLER);

        // When
        Response<BehaviorRebateResponse> result = controller.calendarSignRebate(USER_ID);

        // Then
        var commandCaptor = org.mockito.ArgumentCaptor.forClass(CreateBehaviorRebateOrderCommand.class);
        verify(applicationService).createOrder(commandCaptor.capture());
        CreateBehaviorRebateOrderCommand command = commandCaptor.getValue();
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        assertAll(
                () -> assertEquals(CommonResponseCode.SUCCESS.getCode(), result.code()),
                () -> assertEquals(USER_ID, command.userId()),
                () -> assertEquals("sign", command.behaviorType()),
                () -> assertEquals("sign-" + today + "-" + USER_ID, command.outBusinessNo()),
                () -> assertEquals(List.of(ORDER_ID), result.data().orderIds()),
                () -> assertEquals(List.of(EVENT_ID), result.data().eventIds())
        );
    }

    /**
     * Given 合法用户标识，When 调用日历签到 HTTP 端点，Then 返回统一成功响应并生成签到命令。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldExposeCalendarSignHttpContract() throws Exception {
        // Given
        IBehaviorRebateApplicationService applicationService =
                mock(IBehaviorRebateApplicationService.class);
        when(applicationService.createOrder(org.mockito.ArgumentMatchers.any())).thenReturn(
                new BehaviorRebateOrderCreateResult(List.of(ORDER_ID), List.of(EVENT_ID))
        );
        MockMvc mockMvc = standaloneSetup(new BehaviorRebateController(applicationService, ASSEMBLER))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        // When & Then
        mockMvc.perform(post(IBehaviorRebateApi.BASE_PATH + "/calendar-sign")
                        .param("userId", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.orderIds[0]").value(ORDER_ID))
                .andExpect(jsonPath("$.data.eventIds[0]").value(EVENT_ID));

        var commandCaptor = org.mockito.ArgumentCaptor.forClass(CreateBehaviorRebateOrderCommand.class);
        verify(applicationService).createOrder(commandCaptor.capture());
        CreateBehaviorRebateOrderCommand command = commandCaptor.getValue();
        assertAll(
                () -> assertEquals(USER_ID, command.userId()),
                () -> assertEquals("sign", command.behaviorType()),
                () -> assertTrue(command.outBusinessNo().startsWith("sign-")),
                () -> assertTrue(command.outBusinessNo().endsWith("-" + USER_ID))
        );
    }

    /**
     * Given 不符合契约的 JSON 请求，When 调用 HTTP 端点，Then 返回 400 参数错误。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldRejectInvalidHttpRequest() throws Exception {
        // Given
        MockMvc mockMvc = standaloneSetup(new BehaviorRebateController(
                mock(IBehaviorRebateApplicationService.class),
                ASSEMBLER))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        String requestBody = """
                {
                  "userId": " ",
                  "behaviorType": " ",
                  "outBusinessNo": " "
                }
                """;

        // When & Then
        mockMvc.perform(post(IBehaviorRebateApi.BASE_PATH + "/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.PARAM_INVALID.getCode()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
