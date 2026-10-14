package com.lavyoung.marketforge.app.config;

import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.timelimiter.autoconfigure.TimeLimiterAutoConfiguration;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JAutoConfiguration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 抽奖熔断配置绑定测试。
 *
 * <p>该测试使用最小 Spring 上下文读取真实的开发环境配置，验证
 * {@code activity-draw} 实例的熔断和超时参数已经进入 Resilience4j 注册表，
 * 避免 YAML 键名拼写错误或配置层级错误导致运行时退回默认值。</p>
 */
class ActivityDrawCircuitBreakerConfigurationTest {

    private static final String INSTANCE_NAME = "activity-draw";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withConfiguration(AutoConfigurations.of(
                    CircuitBreakerAutoConfiguration.class,
                    TimeLimiterAutoConfiguration.class,
                    Resilience4JAutoConfiguration.class
            ))
            .withPropertyValues("spring.profiles.active=dev");

    /**
     * 验证抽奖熔断器读取计数窗口、最小调用数、失败率和慢调用参数，
     * 同时确认业务异常不会被计入系统故障。
     */
    @Test
    void shouldBindActivityDrawCircuitBreakerConfiguration() {
        contextRunner.run(context -> {
            // Given: 开发环境配置已经由最小 Spring 上下文加载。
            CircuitBreakerRegistry registry = context.getBean(CircuitBreakerRegistry.class);

            // When: 按抽奖保护实例名称获取最终生效配置。
            CircuitBreakerConfig config = registry.circuitBreaker(INSTANCE_NAME).getCircuitBreakerConfig();

            // Then: 核心阈值与 application-dev.yml 保持一致。
            assertThat(config.getSlidingWindowType()).isEqualTo(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED);
            assertThat(config.getSlidingWindowSize()).isEqualTo(10);
            assertThat(config.getMinimumNumberOfCalls()).isEqualTo(5);
            assertThat(config.getFailureRateThreshold()).isEqualTo(50.0f);
            assertThat(config.getSlowCallRateThreshold()).isEqualTo(50.0f);
            assertThat(config.getSlowCallDurationThreshold()).isEqualTo(Duration.ofSeconds(2));
            assertThat(config.getWaitIntervalFunctionInOpenState().apply(1)).isEqualTo(10_000L);
            assertThat(config.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(3);
            assertThat(config.isAutomaticTransitionFromOpenToHalfOpenEnabled()).isTrue();
            assertThat(config.getRecordExceptionPredicate().test(new IllegalStateException("system failure"))).isTrue();
            assertThat(config.getIgnoreExceptionPredicate().test(
                    BusinessException.of(BusinessResponseCode.ACTIVITY_ACCOUNT_QUOTA_NOT_ENOUGH)
            )).isTrue();
        });
    }

    /**
     * 验证抽奖超时配置进入 TimeLimiter 注册表，确保同步保护适配器创建实例时
     * 能使用五秒超时和执行任务取消策略。
     */
    @Test
    void shouldBindActivityDrawTimeLimiterConfiguration() {
        contextRunner.run(context -> {
            // Given: 开发环境中的 TimeLimiter 配置已经加载。
            TimeLimiterRegistry registry = context.getBean(TimeLimiterRegistry.class);

            // When: 按抽奖保护实例名称获取最终生效配置。
            TimeLimiterConfig config = registry.timeLimiter(INSTANCE_NAME).getTimeLimiterConfig();

            // Then: 超时时间与取消策略均应符合抽奖入口约定。
            assertThat(config.getTimeoutDuration()).isEqualTo(Duration.ofSeconds(5));
            assertThat(config.shouldCancelRunningFuture()).isTrue();
        });
    }
}
