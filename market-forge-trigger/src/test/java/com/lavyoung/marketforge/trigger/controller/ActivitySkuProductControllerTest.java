package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.activity.IActivitySkuProductApi;
import com.lavyoung.marketforge.application.activity.model.ActivitySkuProductResult;
import com.lavyoung.marketforge.application.activity.service.IActivitySkuProductApplicationService;
import com.lavyoung.marketforge.trigger.assembler.ActivitySkuProductResponseAssembler;
import com.lavyoung.marketforge.trigger.exception.GlobalExceptionHandler;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import com.lavyoung.marketforge.types.model.Response;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * 活动 SKU 商品查询 Controller 测试。
 *
 * <p>测试 Controller 是否只负责调用应用服务、转换响应并暴露稳定 HTTP 契约。</p>
 */
class ActivitySkuProductControllerTest {

    private static final Long ACTIVITY_ID = 100301L;
    private static final Long SKU = 9011L;
    private static final ActivitySkuProductResponseAssembler ASSEMBLER =
            Mappers.getMapper(ActivitySkuProductResponseAssembler.class);

    /**
     * Given 应用服务返回商品结果，When 直接调用 Controller，Then 应返回统一成功响应。
     */
    @Test
    void shouldWrapSkuProductsAsSuccessResponse() {
        // Given
        IActivitySkuProductApplicationService applicationService =
                mock(IActivitySkuProductApplicationService.class);
        when(applicationService.querySkuProducts(ACTIVITY_ID)).thenReturn(List.of(productResult()));
        ActivitySkuProductController controller =
                new ActivitySkuProductController(applicationService, ASSEMBLER);

        // When
        Response<?> response = controller.querySkuProducts(ACTIVITY_ID);

        // Then
        org.junit.jupiter.api.Assertions.assertEquals(CommonResponseCode.SUCCESS.getCode(), response.code());
        org.junit.jupiter.api.Assertions.assertEquals(1, ((List<?>) response.data()).size());
        verify(applicationService).querySkuProducts(ACTIVITY_ID);
    }

    /**
     * Given 合法活动标识，When 调用 HTTP 接口，Then 应返回商品字段和统一成功码。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldExposeSkuProductHttpContract() throws Exception {
        // Given
        IActivitySkuProductApplicationService applicationService =
                mock(IActivitySkuProductApplicationService.class);
        when(applicationService.querySkuProducts(ACTIVITY_ID)).thenReturn(List.of(productResult()));
        MockMvc mockMvc = standaloneSetup(new ActivitySkuProductController(applicationService, ASSEMBLER))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        // When & Then
        mockMvc.perform(get(IActivitySkuProductApi.BASE_PATH).param("activityId", ACTIVITY_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data[0].sku").value(SKU))
                .andExpect(jsonPath("$.data[0].stockCountSurplus").value(99888))
                .andExpect(jsonPath("$.data[0].creditAmount").value(100));

        verify(applicationService).querySkuProducts(ACTIVITY_ID);
    }

    /**
     * 创建商品查询结果测试数据。
     *
     * @return 商品查询结果
     */
    private ActivitySkuProductResult productResult() {
        return new ActivitySkuProductResult(SKU, ACTIVITY_ID, 100000, 99888, 100, 10, 100, 100L);
    }
}
