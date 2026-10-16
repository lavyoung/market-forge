package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.award.IUserAwardRecordApi;
import com.lavyoung.marketforge.application.award.model.UserAwardRecordSearchItem;
import com.lavyoung.marketforge.application.award.model.UserAwardRecordSearchQuery;
import com.lavyoung.marketforge.application.award.service.IUserAwardRecordSearchService;
import com.lavyoung.marketforge.trigger.assembler.UserAwardRecordSearchAssembler;
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
 * {@link UserAwardRecordController} HTTP 契约测试。
 *
 * <p>验证 GET 查询参数绑定、应用服务委派、分页响应结构、请求校验和异常隐藏行为，
 * 不连接真实 Elasticsearch。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/16
 */
class UserAwardRecordControllerTest {

    private static final LocalDateTime START_TIME = LocalDateTime.of(2026, 10, 16, 0, 0);
    private static final LocalDateTime END_TIME = LocalDateTime.of(2026, 10, 17, 0, 0);
    private static final UserAwardRecordSearchAssembler ASSEMBLER =
            Mappers.getMapper(UserAwardRecordSearchAssembler.class);

    /**
     * 验证合法 GET 参数会转换为应用查询，并返回统一分页响应。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldExposeUserAwardRecordSearchHttpContract() throws Exception {
        // Given
        IUserAwardRecordSearchService searchService = mock(IUserAwardRecordSearchService.class);
        UserAwardRecordSearchQuery expectedQuery = new UserAwardRecordSearchQuery(
                "user-001", 101L, "completed", START_TIME, END_TIME, 1, 20
        );
        when(searchService.search(expectedQuery)).thenReturn(
                new PageResult<>(List.of(searchItem()), 1, 1, 20)
        );
        MockMvc mockMvc = mockMvc(searchService);

        // When / Then
        mockMvc.perform(get(IUserAwardRecordApi.BASE_PATH)
                        .param("userId", "user-001")
                        .param("awardId", "101")
                        .param("awardState", "completed")
                        .param("startTime", "2026-10-16T00:00:00")
                        .param("endTime", "2026-10-17T00:00:00")
                        .param("pageNo", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.pageNumber").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20))
                .andExpect(jsonPath("$.data.records[0].orderId")
                        .value("ROPmv8av9hj0002z38fvn2949wettup"))
                .andExpect(jsonPath("$.data.records[0].userId").value("user-001"))
                .andExpect(jsonPath("$.data.records[0].awardId").value(101))
                .andExpect(jsonPath("$.data.records[0].awardState").value("completed"));

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
        IUserAwardRecordSearchService searchService = mock(IUserAwardRecordSearchService.class);
        MockMvc mockMvc = mockMvc(searchService);

        // When / Then
        mockMvc.perform(get(IUserAwardRecordApi.BASE_PATH)
                        .param("pageNo", "0")
                        .param("pageSize", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.PARAM_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value("页码不能小于 1"));

        verifyNoInteractions(searchService);
    }

    /**
     * 验证不存在的发奖状态会在协议校验阶段被拒绝。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldRejectUnsupportedAwardState() throws Exception {
        // Given
        IUserAwardRecordSearchService searchService = mock(IUserAwardRecordSearchService.class);
        MockMvc mockMvc = mockMvc(searchService);

        // When / Then
        mockMvc.perform(get(IUserAwardRecordApi.BASE_PATH)
                        .param("awardState", "unknown")
                        .param("pageNo", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.PARAM_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value("发奖状态不合法"));

        verifyNoInteractions(searchService);
    }

    /**
     * 验证起止时间相等时不构成有效的左闭右开查询区间。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldRejectEqualSearchTimeBoundary() throws Exception {
        // Given
        IUserAwardRecordSearchService searchService = mock(IUserAwardRecordSearchService.class);
        MockMvc mockMvc = mockMvc(searchService);

        // When / Then
        mockMvc.perform(get(IUserAwardRecordApi.BASE_PATH)
                        .param("startTime", "2026-10-16T00:00:00")
                        .param("endTime", "2026-10-16T00:00:00")
                        .param("pageNo", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.PARAM_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value("中奖时间起点必须早于终点"));

        verifyNoInteractions(searchService);
    }

    /**
     * 验证查询起点晚于终点时在 HTTP 协议层直接拒绝请求。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldRejectReversedSearchTimeBoundary() throws Exception {
        // Given
        IUserAwardRecordSearchService searchService = mock(IUserAwardRecordSearchService.class);
        MockMvc mockMvc = mockMvc(searchService);

        // When / Then
        mockMvc.perform(get(IUserAwardRecordApi.BASE_PATH)
                        .param("startTime", "2026-10-17T00:00:00")
                        .param("endTime", "2026-10-16T00:00:00")
                        .param("pageNo", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.PARAM_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value("中奖时间起点必须早于终点"));

        verifyNoInteractions(searchService);
    }

    /**
     * 验证未预期的应用异常返回系统错误，且不会向调用方泄露内部异常信息。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldHideUnexpectedApplicationException() throws Exception {
        // Given
        IUserAwardRecordSearchService searchService = mock(IUserAwardRecordSearchService.class);
        when(searchService.search(new UserAwardRecordSearchQuery(
                null, null, null, null, null, 1, 20
        ))).thenThrow(new IllegalStateException("elasticsearch internal detail"));
        MockMvc mockMvc = mockMvc(searchService);

        // When / Then
        mockMvc.perform(get(IUserAwardRecordApi.BASE_PATH)
                        .param("pageNo", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.SYSTEM_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value("System error"));
    }

    /**
     * 创建包含全局异常处理器的独立 MVC 测试环境。
     *
     * @param searchService 用户中奖记录搜索应用服务桩对象
     * @return 可执行 HTTP 契约测试的 MockMvc
     */
    private MockMvc mockMvc(IUserAwardRecordSearchService searchService) {
        return standaloneSetup(new UserAwardRecordController(searchService, ASSEMBLER))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 构造用户中奖记录应用查询结果。
     *
     * @return 字段完整的用户中奖记录
     */
    private UserAwardRecordSearchItem searchItem() {
        return new UserAwardRecordSearchItem(
                "ROPmv8av9hj0002z38fvn2949wettup",
                "user-001",
                100301L,
                100006L,
                101L,
                "用户积分",
                "completed",
                LocalDateTime.of(2026, 10, 16, 9, 30)
        );
    }
}
