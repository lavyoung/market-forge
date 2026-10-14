package com.lavyoung.marketforge.trigger.circuitbreaker;

import com.lavyoung.marketforge.api.activity.response.ActivityAccountResponse;
import com.lavyoung.marketforge.api.activity.response.ActivityAwardResponse;
import com.lavyoung.marketforge.api.activity.response.ActivityDrawResponse;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.model.Response;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 验证活动熔断保护适配器的场景选择、泛型响应和异常分类行为。
 */
@ExtendWith(MockitoExtension.class)
class ActivityCircuitBreakerTest {

    private static final ActivityProtectionContext DRAW_CONTEXT = new ActivityProtectionContext(
            ActivityProtectionScene.DRAW,
            "user-001",
            100_301L
    );

    @Mock
    private CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    @Mock
    private CircuitBreaker circuitBreaker;

    /**
     * Given 抽奖调用成功，When 经过保护适配器，Then 使用抽奖实例并返回原响应。
     */
    @Test
    void shouldReturnDrawResponseThroughSceneCircuitBreaker() {
        // Given
        ActivityCircuitBreaker protection = passThroughProtection();
        Response<ActivityDrawResponse> expected = Response.success(null);

        // When
        Response<ActivityDrawResponse> actual = protection.execute(DRAW_CONTEXT, () -> expected);

        // Then
        assertSame(expected, actual);
        verify(circuitBreakerFactory).create(ActivityProtectionScene.DRAW.getCircuitBreakerName());
    }

    /**
     * Given 不同活动响应类型，When 经同一泛型保护器执行，Then 均保持各自类型和结果。
     */
    @Test
    void shouldSupportAccountAndAwardListResponseTypes() {
        // Given
        ActivityCircuitBreaker protection = passThroughProtection();
        ActivityProtectionContext accountContext = new ActivityProtectionContext(
                ActivityProtectionScene.QUERY_ACCOUNT,
                "user-001",
                100_301L
        );
        ActivityProtectionContext awardContext = new ActivityProtectionContext(
                ActivityProtectionScene.QUERY_AWARD_LIST,
                "user-001",
                100_301L
        );
        Response<ActivityAccountResponse> accountResponse = Response.success(null);
        Response<List<ActivityAwardResponse>> awardResponse = Response.success(List.of());

        // When
        Response<ActivityAccountResponse> actualAccount = protection.execute(accountContext, () -> accountResponse);
        Response<List<ActivityAwardResponse>> actualAwards = protection.execute(awardContext, () -> awardResponse);

        // Then
        assertSame(accountResponse, actualAccount);
        assertSame(awardResponse, actualAwards);
        verify(circuitBreakerFactory).create(ActivityProtectionScene.QUERY_ACCOUNT.getCircuitBreakerName());
        verify(circuitBreakerFactory).create(ActivityProtectionScene.QUERY_AWARD_LIST.getCircuitBreakerName());
    }

    /**
     * Given 熔断器捕获技术异常，When 执行 fallback，Then 返回活动降级响应。
     */
    @Test
    void shouldReturnDegradedResponseForTechnicalFailure() {
        // Given
        ActivityCircuitBreaker protection = fallbackProtection(new IllegalStateException("database unavailable"));

        // When
        Response<ActivityDrawResponse> response = protection.execute(
                DRAW_CONTEXT,
                () -> Response.success(null)
        );

        // Then
        assertEquals(BusinessResponseCode.ACTIVITY_DEGRADED.getCode(), response.code());
        assertEquals(BusinessResponseCode.ACTIVITY_DEGRADED.getMsg(), response.message());
        assertNull(response.data());
    }

    /**
     * Given TimeLimiter 包装业务异常，When 执行 fallback，Then 解包并原样抛出业务异常。
     */
    @Test
    void shouldRethrowWrappedBusinessException() {
        // Given
        BusinessException expected = BusinessException.of(
                BusinessResponseCode.ACTIVITY_ACCOUNT_QUOTA_NOT_ENOUGH
        );
        ActivityCircuitBreaker protection = fallbackProtection(new CompletionException(expected));

        // When
        BusinessException actual = assertThrows(
                BusinessException.class,
                () -> protection.execute(DRAW_CONTEXT, () -> Response.success(null))
        );

        // Then
        assertSame(expected, actual);
    }

    /**
     * Given 上下文、场景或调用函数缺失，When 执行保护器，Then 创建熔断器前拒绝参数。
     */
    @Test
    void shouldRejectInvalidExecutionArguments() {
        // Given
        ActivityCircuitBreaker protection = new ActivityCircuitBreaker(circuitBreakerFactory);
        ActivityProtectionContext missingScene = new ActivityProtectionContext(null, "user-001", 100_301L);

        // When & Then
        assertThrows(NullPointerException.class, () -> protection.execute(null, () -> Response.success(null)));
        assertThrows(NullPointerException.class, () -> protection.execute(missingScene, () -> Response.success(null)));
        assertThrows(NullPointerException.class, () -> protection.execute(DRAW_CONTEXT, null));
        verifyNoInteractions(circuitBreakerFactory);
    }

    /**
     * Given 熔断器工厂为空，When 创建保护器，Then 立即拒绝无效依赖。
     */
    @Test
    void shouldRejectNullCircuitBreakerFactory() {
        assertThrows(NullPointerException.class, () -> new ActivityCircuitBreaker(null));
    }

    /**
     * Given 抽奖熔断器最少统计五次调用，When 连续发生五次技术异常，Then 熔断器进入 OPEN，
     * 后续请求不再执行正常抽奖函数而直接返回降级响应。
     */
    @Test
    void shouldOpenCircuitAfterMinimumTechnicalFailures() {
        // Given
        CircuitBreakerRegistry registry = circuitBreakerRegistry();
        ActivityCircuitBreaker protection = realProtection(registry);
        AtomicInteger actionInvocations = new AtomicInteger();

        // When: 前五次调用进入业务函数并产生技术异常。
        for (int index = 0; index < 5; index++) {
            Response<ActivityDrawResponse> response = protection.execute(DRAW_CONTEXT, () -> {
                actionInvocations.incrementAndGet();
                throw new IllegalStateException("database unavailable");
            });
            assertEquals(BusinessResponseCode.ACTIVITY_DEGRADED.getCode(), response.code());
        }

        // Then: 第六次调用被 OPEN 状态直接拒绝，不再进入业务函数。
        Response<ActivityDrawResponse> rejected = protection.execute(DRAW_CONTEXT, () -> {
            actionInvocations.incrementAndGet();
            return Response.success(null);
        });
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker =
                registry.circuitBreaker(ActivityProtectionScene.DRAW.getCircuitBreakerName());

        assertEquals(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN, circuitBreaker.getState());
        assertEquals(5, actionInvocations.get());
        assertEquals(BusinessResponseCode.ACTIVITY_DEGRADED.getCode(), rejected.code());
        assertEquals(1, circuitBreaker.getMetrics().getNumberOfNotPermittedCalls());
    }

    /**
     * Given 业务异常被配置为忽略，When 连续发生业务异常，Then 异常原样抛出且熔断器保持 CLOSED。
     */
    @Test
    void shouldKeepCircuitClosedForIgnoredBusinessFailures() {
        // Given
        CircuitBreakerRegistry registry = circuitBreakerRegistry();
        ActivityCircuitBreaker protection = realProtection(registry);
        BusinessException expected = BusinessException.of(
                BusinessResponseCode.ACTIVITY_ACCOUNT_QUOTA_NOT_ENOUGH
        );

        // When
        for (int index = 0; index < 5; index++) {
            BusinessException actual = assertThrows(
                    BusinessException.class,
                    () -> protection.execute(DRAW_CONTEXT, () -> {
                        throw expected;
                    })
            );
            assertSame(expected, actual);
        }

        // Then
        io.github.resilience4j.circuitbreaker.CircuitBreaker circuitBreaker =
                registry.circuitBreaker(ActivityProtectionScene.DRAW.getCircuitBreakerName());
        assertEquals(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.CLOSED, circuitBreaker.getState());
        assertEquals(0, circuitBreaker.getMetrics().getNumberOfFailedCalls());
    }

    private ActivityCircuitBreaker realProtection(CircuitBreakerRegistry circuitBreakerRegistry) {
        Resilience4JCircuitBreakerFactory factory = new Resilience4JCircuitBreakerFactory(
                circuitBreakerRegistry,
                TimeLimiterRegistry.ofDefaults(),
                null
        );
        return new ActivityCircuitBreaker(factory);
    }

    private CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(5)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50.0f)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .ignoreExceptions(BusinessException.class)
                .build();
        return CircuitBreakerRegistry.of(config);
    }

    private ActivityCircuitBreaker passThroughProtection() {
        when(circuitBreakerFactory.create(any(String.class))).thenReturn(circuitBreaker);
        when(circuitBreaker.run(any(), any())).thenAnswer(invocation -> {
            Supplier<?> action = invocation.getArgument(0);
            return action.get();
        });
        return new ActivityCircuitBreaker(circuitBreakerFactory);
    }

    private ActivityCircuitBreaker fallbackProtection(Throwable failure) {
        when(circuitBreakerFactory.create(any(String.class))).thenReturn(circuitBreaker);
        when(circuitBreaker.run(any(), any())).thenAnswer(invocation -> {
            Function<Throwable, ?> fallback = invocation.getArgument(1);
            return fallback.apply(failure);
        });
        return new ActivityCircuitBreaker(circuitBreakerFactory);
    }
}
