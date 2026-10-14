package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.activity.IActivityRaffleApi;
import com.lavyoung.marketforge.api.activity.request.ActivityAwardListRequest;
import com.lavyoung.marketforge.api.activity.request.ActivityDrawRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityAccountResponse;
import com.lavyoung.marketforge.api.activity.response.ActivityDrawResponse;
import com.lavyoung.marketforge.application.activity.model.ActivityAccountResult;
import com.lavyoung.marketforge.application.activity.model.ActivityAwardListCommand;
import com.lavyoung.marketforge.application.activity.model.ActivityRaffleCommand;
import com.lavyoung.marketforge.application.activity.model.ActivityRaffleResult;
import com.lavyoung.marketforge.application.activity.service.IActivityRaffleApplicationService;
import com.lavyoung.marketforge.trigger.assembler.ActivityRaffleResponseAssembler;
import com.lavyoung.marketforge.trigger.circuitbreaker.ActivityCircuitBreaker;
import com.lavyoung.marketforge.trigger.exception.GlobalExceptionHandler;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import com.lavyoung.marketforge.types.model.Response;
import io.github.lavyoung.dcc.ratelimiter.core.*;
import io.github.lavyoung.dcc.ratelimiter.starter.aop.DccRateLimiterAspect;
import io.github.lavyoung.dcc.ratelimiter.starter.switcher.RateLimiterSwitch;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

/**
 * 验证活动抽奖 HTTP 适配器的账户查询契约、参数校验和统一响应包装。
 */
class ActivityRaffleControllerTest {

    private static final String USER_ID = "user-001";
    private static final Long ACTIVITY_ID = 100_301L;
    private static final Long SKU = 9_011L;
    private static final ActivityRaffleResponseAssembler ASSEMBLER =
            Mappers.getMapper(ActivityRaffleResponseAssembler.class);

    /**
     * Given 动态限流开关关闭，When 用户发起抽奖，Then 跳过限流存储并正常执行业务。
     */
    @Test
    void shouldProceedWithDrawWhenRateLimiterIsDisabled() {
        // Given
        IActivityRaffleApplicationService applicationService = mock(IActivityRaffleApplicationService.class);
        RateLimiterStore rateLimiterStore = mock(RateLimiterStore.class);
        ActivityRaffleCommand expectedCommand = new ActivityRaffleCommand(USER_ID, ACTIVITY_ID, SKU);
        when(applicationService.raffle(expectedCommand)).thenReturn(raffleResult());
        IActivityRaffleApi proxy = rateLimitedProxy(applicationService, () -> false, rateLimiterStore);

        // When
        Response<ActivityDrawResponse> response =
                proxy.draw(new ActivityDrawRequest(USER_ID, ACTIVITY_ID, SKU));

        // Then
        assertAll(
                () -> assertEquals(CommonResponseCode.SUCCESS.getCode(), response.code()),
                () -> assertEquals("order-001", response.data().orderId()),
                () -> assertEquals(9_009_010L, response.data().awardId())
        );
        verifyNoInteractions(rateLimiterStore);
        verify(applicationService).raffle(expectedCommand);
    }

    /**
     * Given 动态限流已开启且当前用户被拒绝，When 用户发起抽奖，Then 调用 fallback 且不进入应用服务。
     */
    @Test
    void shouldFallbackWithoutCallingApplicationServiceWhenDrawIsRateLimited() {
        // Given
        IActivityRaffleApplicationService applicationService = mock(IActivityRaffleApplicationService.class);
        RateLimiterStore rateLimiterStore = mock(RateLimiterStore.class);
        when(rateLimiterStore.acquire(eq(USER_ID), any(RateLimiterRule.class)))
                .thenReturn(RateLimiterDecision.rateLimited());
        IActivityRaffleApi proxy = rateLimitedProxy(applicationService, () -> true, rateLimiterStore);

        // When
        Response<ActivityDrawResponse> response =
                proxy.draw(new ActivityDrawRequest(USER_ID, ACTIVITY_ID, SKU));

        // Then
        assertAll(
                () -> assertEquals(BusinessResponseCode.RISK_CONTROL_RATE_LIMITED.getCode(), response.code()),
                () -> assertEquals(BusinessResponseCode.RISK_CONTROL_RATE_LIMITED.getMsg(), response.message()),
                () -> assertNull(response.data())
        );
        verify(rateLimiterStore).acquire(eq(USER_ID), argThat(rule ->
                rule.permitsPerSecond() == 1.0D
                        && rule.blacklistCount() == 3
                        && rule.blacklistTtlSeconds() == 60L));
        verifyNoInteractions(applicationService);
    }

    /**
     * Given 抽奖保护器捕获技术异常，When 用户发起抽奖，Then 返回活动降级响应且不执行应用服务。
     */
    @Test
    void shouldReturnDegradedResponseWhenProtectedDrawFailsTechnically() {
        // Given
        IActivityRaffleApplicationService applicationService =
                mock(IActivityRaffleApplicationService.class);
        ActivityCircuitBreaker protection = degradedProtection();
        ActivityRaffleController controller =
                new ActivityRaffleController(applicationService, ASSEMBLER, protection);

        // When
        Response<ActivityDrawResponse> response =
                controller.draw(new ActivityDrawRequest(USER_ID, ACTIVITY_ID, SKU));

        // Then
        assertAll(
                () -> assertEquals(BusinessResponseCode.ACTIVITY_DEGRADED.getCode(), response.code()),
                () -> assertEquals(BusinessResponseCode.ACTIVITY_DEGRADED.getMsg(), response.message()),
                () -> assertNull(response.data())
        );
        verifyNoInteractions(applicationService);
    }

    /**
     * Given TimeLimiter 包装了业务异常，When fallback 分类异常，Then 解包并原样抛出业务异常。
     */
    @Test
    void shouldRethrowWrappedBusinessExceptionFromProtectionFallback() {
        // Given
        BusinessException businessException = BusinessException.of(
                BusinessResponseCode.ACTIVITY_ACCOUNT_QUOTA_NOT_ENOUGH
        );
        ActivityCircuitBreaker protection = businessFailureProtection(businessException);
        ActivityRaffleController controller = new ActivityRaffleController(
                mock(IActivityRaffleApplicationService.class),
                ASSEMBLER,
                protection
        );

        // When
        BusinessException thrown = assertThrows(
                BusinessException.class,
                () -> controller.draw(new ActivityDrawRequest(USER_ID, ACTIVITY_ID, SKU))
        );

        // Then
        assertSame(businessException, thrown);
    }

    /**
     * Given 合法账户查询请求和固定应用结果，When 直接调用 Controller，Then 转换命令并包装响应。
     */
    @Test
    void shouldQueryActivityAccountAndWrapResponse() {
        // Given
        IActivityRaffleApplicationService applicationService =
                mock(IActivityRaffleApplicationService.class);
        ActivityAwardListCommand expectedCommand = new ActivityAwardListCommand(USER_ID, ACTIVITY_ID);
        when(applicationService.queryActivityAccount(expectedCommand))
                .thenReturn(new ActivityAccountResult(10, 8, 3, 2, 7, 6));
        ActivityRaffleController controller =
                new ActivityRaffleController(applicationService, ASSEMBLER, passThroughProtection());

        // When
        Response<ActivityAccountResponse> result =
                controller.queryActivityAccount(new ActivityAwardListRequest(USER_ID, ACTIVITY_ID));

        // Then
        assertAll(
                () -> assertEquals(CommonResponseCode.SUCCESS.getCode(), result.code()),
                () -> assertEquals(10, result.data().totalCount()),
                () -> assertEquals(8, result.data().totalCountSurplus()),
                () -> assertEquals(3, result.data().dayCount()),
                () -> assertEquals(2, result.data().dayCountSurplus()),
                () -> assertEquals(7, result.data().monthCount()),
                () -> assertEquals(6, result.data().monthCountSurplus())
        );
        verify(applicationService).queryActivityAccount(expectedCommand);
    }

    /**
     * Given 必填字段为空或非法，When 执行 Bean Validation，Then 用户标识和活动标识约束都应生效。
     */
    @Test
    void shouldRejectStructurallyInvalidAccountRequest() {
        // Given
        try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = validatorFactory.getValidator();
            ActivityAwardListRequest request = new ActivityAwardListRequest(" ", 0L);

            // When
            var violations = validator.validate(request);

            // Then
            assertEquals(2, violations.size());
        }
    }

    /**
     * Given 合法 JSON 请求，When 调用活动账户 HTTP 端点，Then 返回统一成功响应和账户额度。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldExposeActivityAccountHttpContract() throws Exception {
        // Given
        IActivityRaffleApplicationService applicationService =
                mock(IActivityRaffleApplicationService.class);
        when(applicationService.queryActivityAccount(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ActivityAccountResult(10, 8, 3, 2, 7, 6));
        MockMvc mockMvc = standaloneSetup(new ActivityRaffleController(
                applicationService,
                ASSEMBLER,
                passThroughProtection()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        String requestBody = """
                {
                  "userId": "user-001",
                  "activityId": 100301
                }
                """;

        // When & Then
        mockMvc.perform(post(IActivityRaffleApi.BASE_PATH + "/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.SUCCESS.getCode()))
                .andExpect(jsonPath("$.data.totalCount").value(10))
                .andExpect(jsonPath("$.data.totalCountSurplus").value(8))
                .andExpect(jsonPath("$.data.dayCount").value(3))
                .andExpect(jsonPath("$.data.dayCountSurplus").value(2))
                .andExpect(jsonPath("$.data.monthCount").value(7))
                .andExpect(jsonPath("$.data.monthCountSurplus").value(6));
    }

    /**
     * Given 不符合契约的 JSON 请求，When 调用活动账户 HTTP 端点，Then 返回 400 参数错误。
     *
     * @throws Exception MockMvc 请求执行失败时抛出
     */
    @Test
    void shouldRejectInvalidActivityAccountHttpRequest() throws Exception {
        // Given
        MockMvc mockMvc = standaloneSetup(new ActivityRaffleController(
                mock(IActivityRaffleApplicationService.class),
                ASSEMBLER,
                passThroughProtection()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        String requestBody = """
                {
                  "userId": " ",
                  "activityId": 0
                }
                """;

        // When & Then
        mockMvc.perform(post(IActivityRaffleApi.BASE_PATH + "/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CommonResponseCode.PARAM_INVALID.getCode()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /**
     * 创建带有真实限流切面的 Controller 代理。
     *
     * @param applicationService 抽奖应用服务
     * @param rateLimiterSwitch  动态限流开关
     * @param rateLimiterStore   限流状态存储
     * @return 实现活动抽奖 API 的 AOP 代理
     */
    private IActivityRaffleApi rateLimitedProxy(
            IActivityRaffleApplicationService applicationService,
            RateLimiterSwitch rateLimiterSwitch,
            RateLimiterStore rateLimiterStore
    ) {
        ActivityCircuitBreaker protection = passThroughProtection();
        ActivityRaffleController target = new ActivityRaffleController(
                applicationService,
                ASSEMBLER,
                protection
        );
        DccRateLimiterAspect aspect = new DccRateLimiterAspect(
                rateLimiterSwitch,
                rateLimiterStore,
                new RateLimiterKeyResolver(),
                new RateLimiterFallbackInvoker()
        );
        AspectJProxyFactory proxyFactory = new AspectJProxyFactory(target);
        proxyFactory.addAspect(aspect);
        return proxyFactory.getProxy();
    }

    /**
     * 创建执行正常调用函数的保护器测试桩。
     *
     * @return 正常放行的保护器
     */
    private ActivityCircuitBreaker passThroughProtection() {
        ActivityCircuitBreaker protection = mock(ActivityCircuitBreaker.class);
        when(protection.execute(any(), any())).thenAnswer(invocation -> {
            Supplier<?> action = invocation.getArgument(1);
            return action.get();
        });
        return protection;
    }

    /**
     * 创建直接返回活动降级响应的保护器测试桩。
     *
     * @return 返回活动降级结果的保护器
     */
    private ActivityCircuitBreaker degradedProtection() {
        ActivityCircuitBreaker protection = mock(ActivityCircuitBreaker.class);
        when(protection.execute(any(), any()))
                .thenReturn(Response.fail(BusinessResponseCode.ACTIVITY_DEGRADED));
        return protection;
    }

    /**
     * 创建原样抛出业务异常的保护器测试桩。
     *
     * @param businessException 预期抛出的业务异常
     * @return 抛出业务异常的保护器
     */
    private ActivityCircuitBreaker businessFailureProtection(BusinessException businessException) {
        ActivityCircuitBreaker protection = mock(ActivityCircuitBreaker.class);
        when(protection.execute(any(), any())).thenThrow(businessException);
        return protection;
    }

    /**
     * 创建供抽奖成功路径使用的固定应用结果。
     *
     * @return 固定抽奖结果
     */
    private ActivityRaffleResult raffleResult() {
        return new ActivityRaffleResult(
                "order-001",
                ACTIVITY_ID,
                9_009_001L,
                9_009_010L,
                "credit",
                "100",
                "100 积分",
                "测试奖品"
        );
    }
}
