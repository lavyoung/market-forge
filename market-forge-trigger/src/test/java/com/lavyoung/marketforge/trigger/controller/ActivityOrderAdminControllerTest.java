package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.activity.IActivityOrderAdminApi;
import com.lavyoung.marketforge.application.activity.model.ActivityOrderSearchItem;
import com.lavyoung.marketforge.application.activity.model.ActivityOrderSearchQuery;
import com.lavyoung.marketforge.application.activity.service.IActivityOrderSearchService;
import com.lavyoung.marketforge.trigger.assembler.ActivityOrderSearchAssembler;
import com.lavyoung.marketforge.trigger.exception.GlobalExceptionHandler;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import com.lavyoung.marketforge.types.model.PageResult;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * {@link ActivityOrderAdminController} HTTP 契约测试。
 *
 * <p>验证 GET 参数绑定、应用服务委派、分页响应结构、请求校验和异常隐藏行为，
 * 不连接真实 Elasticsearch。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/18
 */
class ActivityOrderAdminControllerTest {

    private static final LocalDateTime START_TIME = LocalDateTime.of(2026, 10, 18, 0, 0);
    private static final LocalDateTime END_TIME = LocalDateTime.of(2026, 10, 19, 0, 0);
    private static final ActivityOrderSearchAssembler ASSEMBLER = Mappers.getMapper(ActivityOrderSearchAssembler.class);

    /**
     * 验证合法 GET 参数会转换为应用查询，并返回统一分页响应和字符串 ID。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldExposeActivityOrderSearchHttpContract() throws Exception {
        // Given
        IActivityOrderSearchService searchService = mock(IActivityOrderSearchService.class);
        ActivityOrderSearchQuery expectedQuery = new ActivityOrderSearchQuery(
                "user-001", 100301L, "order-001", "used", START_TIME, END_TIME, 1, 20
        );
        when(searchService.search(expectedQuery)).thenReturn(
                new PageResult<>(List.of(searchItem()), 1, 1, 20)
        );
        MockMvc mockMvc = mockMvc(searchService);

        // When / Then
        mockMvc.perform(get(IActivityOrderAdminApi.BASE_PATH)
                        .param("userId", "user-001")
                        .param("activityId", "100301")
                        .param("orderId", "order-001")
                        .param("orderState", "used")
                        .param("startTime", "2026-10-18T00:00:00")
                        .param("endTime", "2026-10-19T00:00:00")
                        .param("pageNumber", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.pageNumber").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.records[0].orderId").value("order-001"))
                .andExpect(jsonPath("$.data.records[0].userId").value("user-001"))
                .andExpect(jsonPath("$.data.records[0].activityId").value("9007199254740993"))
                .andExpect(jsonPath("$.data.records[0].sku").value("9007199254740994"))
                .andExpect(jsonPath("$.data.records[0].strategyId").value("9007199254740995"))
                .andExpect(jsonPath("$.data.records[0].orderState").value("used"));

        verify(searchService).search(expectedQuery);
    }

    /**
     * 验证页码非法时返回 HTTP 400，且不会调用应用服务。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldRejectInvalidPageNumberBeforeCallingApplicationService() throws Exception {
        // Given
        IActivityOrderSearchService searchService = mock(IActivityOrderSearchService.class);
        MockMvc mockMvc = mockMvc(searchService);

        // When / Then
        mockMvc.perform(get(IActivityOrderAdminApi.BASE_PATH)
                        .param("pageNumber", "0")
                        .param("pageSize", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.PARAM_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value("页码不能小于 1"));

        verifyNoInteractions(searchService);
    }

    /**
     * 验证未知订单状态会在 HTTP 参数校验阶段被拒绝。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldRejectUnsupportedOrderState() throws Exception {
        // Given
        IActivityOrderSearchService searchService = mock(IActivityOrderSearchService.class);
        MockMvc mockMvc = mockMvc(searchService);

        // When / Then
        mockMvc.perform(get(IActivityOrderAdminApi.BASE_PATH)
                        .param("orderState", "unknown")
                        .param("pageNumber", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.PARAM_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value("订单状态不合法"));

        verifyNoInteractions(searchService);
    }

    /**
     * 验证相等的起止时间不构成有效的左闭右开查询区间。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldRejectEqualSearchTimeBoundary() throws Exception {
        // Given
        IActivityOrderSearchService searchService = mock(IActivityOrderSearchService.class);
        MockMvc mockMvc = mockMvc(searchService);

        // When / Then
        mockMvc.perform(get(IActivityOrderAdminApi.BASE_PATH)
                        .param("startTime", "2026-10-18T00:00:00")
                        .param("endTime", "2026-10-18T00:00:00")
                        .param("pageNumber", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.PARAM_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value("下单时间起点必须早于终点"));

        verifyNoInteractions(searchService);
    }

    /**
     * 验证未预期的应用异常返回系统错误，且不会泄露内部异常信息。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldHideUnexpectedApplicationException() throws Exception {
        // Given
        IActivityOrderSearchService searchService = mock(IActivityOrderSearchService.class);
        when(searchService.search(new ActivityOrderSearchQuery(
                null, null, null, null, null, null, 1, 20
        ))).thenThrow(new IllegalStateException("elasticsearch internal detail"));
        MockMvc mockMvc = mockMvc(searchService);

        // When / Then
        mockMvc.perform(get(IActivityOrderAdminApi.BASE_PATH)
                        .param("pageNumber", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.SYSTEM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("System error"));
    }

    /**
     * 创建包含统一异常处理器的独立 MVC 测试环境。
     *
     * @param searchService 活动订单搜索应用服务桩对象
     * @return 可执行 HTTP 契约测试的 MockMvc
     */
    private MockMvc mockMvc(IActivityOrderSearchService searchService) {
        return standaloneSetup(new ActivityOrderAdminController(searchService, ASSEMBLER))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 构造活动订单应用查询结果。
     *
     * @return 字段完整且标识超过 JavaScript 安全整数范围的活动订单
     */
    private ActivityOrderSearchItem searchItem() {
        return new ActivityOrderSearchItem(
                "user-001",
                9_007_199_254_740_993L,
                9_007_199_254_740_994L,
                "周末幸运抽奖",
                9_007_199_254_740_995L,
                "order-001",
                LocalDateTime.of(2026, 10, 18, 10, 8, 5),
                10,
                1,
                10,
                "used",
                "raffle-001",
                LocalDateTime.of(2026, 10, 18, 10, 8, 5),
                LocalDateTime.of(2026, 10, 18, 10, 8, 7)
        );
    }
}
