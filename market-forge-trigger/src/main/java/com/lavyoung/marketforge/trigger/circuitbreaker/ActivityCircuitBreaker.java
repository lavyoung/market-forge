package com.lavyoung.marketforge.trigger.circuitbreaker;

import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * 活动接口熔断保护适配器。
 *
 * <p>通过 Spring Cloud Circuit Breaker 统一保护返回 {@link Response} 的
 * 活动接口。具体超时、慢调用统计和熔断状态机由 Resilience4j 实现。</p>
 *
 * <p>预期业务异常会继续交给全局异常处理器；超时、熔断打开以及其他
 * 技术异常统一转换为活动降级响应。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/14
 */
@Slf4j
@Component
public class ActivityCircuitBreaker {

    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;

    /**
     * 创建活动接口熔断保护适配器。
     *
     * @param circuitBreakerFactory Spring Cloud 熔断器工厂
     * @throws NullPointerException 熔断器工厂为空时抛出
     */
    public ActivityCircuitBreaker(CircuitBreakerFactory<?, ?> circuitBreakerFactory) {
        this.circuitBreakerFactory = Objects.requireNonNull(circuitBreakerFactory, "circuitBreakerFactory must not be null");
    }

    /**
     * 在指定熔断策略下执行活动接口调用。
     *
     * @param context 当前接口的保护上下文
     * @param action  需要保护的业务调用
     * @param <T>     统一响应中承载的业务数据类型
     * @return 正常业务响应或活动降级响应
     * @throws NullPointerException context、scene 或 action 为空时抛出
     * @throws BusinessException    正常调用产生业务异常时原样抛出
     */
    public <T> Response<T> execute(ActivityProtectionContext context, Supplier<Response<T>> action) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(context.scene(), "context.scene must not be null");
        Objects.requireNonNull(action, "action must not be null");

        CircuitBreaker circuitBreaker = circuitBreakerFactory.create(
                context.scene().getCircuitBreakerName()
        );
        return circuitBreaker.run(action, throwable -> fallback(context, throwable));
    }

    /**
     * 将技术异常转换为统一活动降级响应。
     *
     * @param context   当前接口的保护上下文
     * @param throwable 熔断器捕获的异常
     * @param <T>       统一响应中承载的业务数据类型
     * @return 不包含业务数据的活动降级响应
     * @throws BusinessException 根因属于业务异常时原样抛出
     */
    private <T> Response<T> fallback(ActivityProtectionContext context, Throwable throwable) {
        Throwable cause = unwrapProtectionException(throwable);
        if (cause instanceof BusinessException businessException) {
            throw businessException;
        }

        log.warn("活动接口触发超时或熔断降级 scene={} userId={} activityId={} cause={}", context.scene(), context.userId(),
                context.activityId(),
                cause.getClass().getSimpleName(),
                cause
        );
        return Response.fail(BusinessResponseCode.ACTIVITY_DEGRADED);
    }

    /**
     * 解包异步执行过程中产生的包装异常。
     *
     * @param throwable 熔断器捕获的异常
     * @return 可用于业务分类的根异常
     * @throws NullPointerException throwable 为空时抛出
     */
    private Throwable unwrapProtectionException(Throwable throwable) {
        Throwable current = Objects.requireNonNull(throwable, "throwable must not be null");
        while ((current instanceof CompletionException || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
