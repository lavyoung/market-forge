package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.strategy.IStrategyAdminApi;
import com.lavyoung.marketforge.api.strategy.response.StrategySnapshotRefreshResponse;
import com.lavyoung.marketforge.application.strategy.service.IStrategySnapshotRefreshApplicationService;
import com.lavyoung.marketforge.trigger.exception.GlobalExceptionHandler;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.RejectedExecutionException;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * {@link StrategyAdminController} HTTP 契约测试。
 *
 * <p>验证刷新请求的新任务和合并语义、HTTP 202 状态、路径参数校验以及线程池
 * 拒绝异常的统一隐藏行为，不执行真实策略装配。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
class StrategyAdminControllerTest {

    private static final Long STRATEGY_ID = 9_007_199_254_740_993L;

    /**
     * Given 策略当前没有刷新任务，When 运营端提交刷新命令，Then 返回 202 和新任务标记。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldAcceptNewSnapshotRefreshTask() throws Exception {
        // Given
        IStrategySnapshotRefreshApplicationService applicationService = mock(IStrategySnapshotRefreshApplicationService.class);
        when(applicationService.requestRefresh(STRATEGY_ID)).thenReturn(true);
        MockMvc mockMvc = mockMvc(applicationService);

        // When / Then
        mockMvc.perform(post(IStrategyAdminApi.BASE_PATH + "/{strategyId}/snapshot-refresh", STRATEGY_ID))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.strategyId").value(STRATEGY_ID.toString()))
                .andExpect(jsonPath("$.data.newTaskScheduled").value(true));

        verify(applicationService).requestRefresh(STRATEGY_ID);
    }

    /**
     * Given 策略已经存在刷新任务，When 再次提交刷新命令，Then 请求仍被受理并返回合并标记。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldReportMergedSnapshotRefreshRequest() throws Exception {
        // Given
        IStrategySnapshotRefreshApplicationService applicationService = mock(IStrategySnapshotRefreshApplicationService.class);
        when(applicationService.requestRefresh(STRATEGY_ID)).thenReturn(false);
        MockMvc mockMvc = mockMvc(applicationService);

        // When / Then
        mockMvc.perform(post(IStrategyAdminApi.BASE_PATH + "/{strategyId}/snapshot-refresh", STRATEGY_ID))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.strategyId").value(STRATEGY_ID.toString()))
                .andExpect(jsonPath("$.data.newTaskScheduled").value(false));

        verify(applicationService).requestRefresh(STRATEGY_ID);
    }

    /**
     * Given 策略标识不是正数，When 运营端提交刷新命令，Then HTTP 层返回参数错误且不调用应用服务。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldRejectNonPositiveStrategyId() throws Exception {
        // Given
        IStrategySnapshotRefreshApplicationService applicationService = mock(IStrategySnapshotRefreshApplicationService.class);
        MockMvc mockMvc = mockMvc(applicationService);

        // When / Then
        mockMvc.perform(post(IStrategyAdminApi.BASE_PATH + "/{strategyId}/snapshot-refresh", 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.PARAM_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value("Parameter validation failed"));

        verifyNoInteractions(applicationService);
    }

    /**
     * Given 刷新线程池容量耗尽，When 应用服务拒绝调度任务，Then 返回服务暂不可用且不泄露线程池细节。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldHideExecutorRejectionDetails() throws Exception {
        // Given
        IStrategySnapshotRefreshApplicationService applicationService = mock(IStrategySnapshotRefreshApplicationService.class);
        when(applicationService.requestRefresh(STRATEGY_ID)).thenThrow(new RejectedExecutionException("strategy-refresh queue full"));
        MockMvc mockMvc = mockMvc(applicationService);

        // When / Then
        mockMvc.perform(post(IStrategyAdminApi.BASE_PATH + "/{strategyId}/snapshot-refresh", STRATEGY_ID))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.SERVICE_UNAVAILABLE.getCode()))
                .andExpect(jsonPath("$.message").value(CommonResponseCode.SERVICE_UNAVAILABLE.getMsg()))
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, "1"));
    }

    /**
     * Given 空值、零、负数和非数字策略标识，When 创建刷新响应，Then API 契约立即拒绝非法标识。
     */
    @Test
    void shouldRejectInvalidResponseStrategyIdentifiers() {
        // Given / When / Then
        assertAll(
                () -> assertThrows(NullPointerException.class, () -> new StrategySnapshotRefreshResponse(null, true)),
                () -> assertThrows(IllegalArgumentException.class, () -> new StrategySnapshotRefreshResponse("0", true)),
                () -> assertThrows(IllegalArgumentException.class, () -> new StrategySnapshotRefreshResponse("-1", true)),
                () -> assertThrows(IllegalArgumentException.class, () -> new StrategySnapshotRefreshResponse("strategy-1", true))
        );
    }

    /**
     * 创建包含统一异常处理器和方法参数校验的独立 MVC 测试环境。
     *
     * @param applicationService 策略快照刷新应用服务桩对象
     * @return 可执行 HTTP 契约测试的 MockMvc
     */
    private MockMvc mockMvc(IStrategySnapshotRefreshApplicationService applicationService) {
        return standaloneSetup(new StrategyAdminController(applicationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }
}
