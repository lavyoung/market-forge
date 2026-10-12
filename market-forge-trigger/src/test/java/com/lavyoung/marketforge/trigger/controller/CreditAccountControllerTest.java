package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.credit.ICreditAccountApi;
import com.lavyoung.marketforge.application.credit.model.CreditAccountResult;
import com.lavyoung.marketforge.application.credit.service.ICreditAccountApplicationService;
import com.lavyoung.marketforge.trigger.assembler.CreditAccountResponseAssembler;
import com.lavyoung.marketforge.trigger.exception.GlobalExceptionHandler;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import com.lavyoung.marketforge.types.model.Response;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * 用户积分账户查询 Controller 测试。
 *
 * <p>测试 Controller 是否正确完成 application 结果到 API 响应的协议转换，
 * 同时确认 HTTP 接口返回统一响应结构。</p>
 */
class CreditAccountControllerTest {

    private static final String USER_ID = "user-001";
    private static final CreditAccountResponseAssembler ASSEMBLER =
            Mappers.getMapper(CreditAccountResponseAssembler.class);

    /**
     * Given 应用服务返回账户余额，When 直接调用 Controller，Then 应返回统一成功响应。
     */
    @Test
    void shouldWrapCreditAccountAsSuccessResponse() {
        // Given
        ICreditAccountApplicationService applicationService = mock(ICreditAccountApplicationService.class);
        when(applicationService.queryCreditAccount(USER_ID))
                .thenReturn(new CreditAccountResult(USER_ID, 800L));
        CreditAccountController controller = new CreditAccountController(applicationService, ASSEMBLER);

        // When
        Response<?> response = controller.queryCreditAccount(USER_ID);

        // Then
        assertEquals(CommonResponseCode.SUCCESS.getCode(), response.code());
        assertEquals(USER_ID, ((com.lavyoung.marketforge.api.credit.response.UserCreditAccountResponse)
                response.data()).userId());
        verify(applicationService).queryCreditAccount(USER_ID);
    }

    /**
     * Given 合法用户标识，When 调用 HTTP 接口，Then 应返回可用积分余额。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldExposeCreditAccountHttpContract() throws Exception {
        // Given
        ICreditAccountApplicationService applicationService = mock(ICreditAccountApplicationService.class);
        when(applicationService.queryCreditAccount(USER_ID))
                .thenReturn(new CreditAccountResult(USER_ID, 800L));
        MockMvc mockMvc = standaloneSetup(new CreditAccountController(applicationService, ASSEMBLER))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        // When & Then
        mockMvc.perform(get(ICreditAccountApi.BASE_PATH).param("userId", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.userId").value(USER_ID))
                .andExpect(jsonPath("$.data.availableAmount").value(800));

        verify(applicationService).queryCreditAccount(USER_ID);
    }
}
