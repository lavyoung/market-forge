package com.lavyoung.marketforge.infrastructure.persistent.repository.config;

import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.ProbabilitySnapshotKey;
import com.lavyoung.marketforge.infrastructure.persistent.repository.cache.ProbabilitySnapshotLocalCache;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证概率快照本地缓存的 Spring 配置绑定、Bean 注册和故障回退开关。
 *
 * <p>测试使用最小应用上下文，只加载本地缓存相关配置，不连接 Redis、MySQL、
 * RabbitMQ 或 Elasticsearch。这样既能验证 Spring 真实装配行为，也不会让测试
 * 结果依赖外部开发环境。</p>
 */
class ProbabilitySnapshotLocalCacheConfigurationTest {

    private static final String PREFIX = "market-forge.strategy.snapshot.local-cache";
    private static final ProbabilitySnapshotKey SNAPSHOT_KEY = new ProbabilitySnapshotKey("100001", 7L);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ProbabilitySnapshotLocalCacheConfiguration.class)
            .withBean(ProbabilitySnapshotLocalCache.class);

    /**
     * Given 没有提供外部配置，When Spring 创建最小上下文，Then 使用默认参数注册唯一缓存 Bean。
     */
    @Test
    void shouldRegisterLocalCacheWithDefaultProperties() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ProbabilitySnapshotLocalCacheProperties.class);
            assertThat(context).hasSingleBean(ProbabilitySnapshotLocalCache.class);

            ProbabilitySnapshotLocalCacheProperties properties =
                    context.getBean(ProbabilitySnapshotLocalCacheProperties.class);
            assertThat(properties.enabled()).isTrue();
            assertThat(properties.expireAfterAccess()).isEqualTo(Duration.ofMinutes(30));
            assertThat(properties.maximumSnapshots()).isEqualTo(10_000L);
            assertThat(properties.maximumDataElements()).isEqualTo(100_000L);
        });
    }

    /**
     * Given application.yml 等价的自定义参数，When Spring 完成绑定，Then 缓存 Bean 使用全部外部配置。
     */
    @Test
    void shouldBindApplicationPropertiesAndRegisterLocalCache() {
        contextRunner.withPropertyValues(
                PREFIX + ".enabled=true",
                PREFIX + ".expire-after-access=15m",
                PREFIX + ".maximum-snapshots=500",
                PREFIX + ".maximum-data-elements=5000"
        ).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ProbabilitySnapshotLocalCache.class);

            ProbabilitySnapshotLocalCacheProperties properties =
                    context.getBean(ProbabilitySnapshotLocalCacheProperties.class);
            assertThat(properties.enabled()).isTrue();
            assertThat(properties.expireAfterAccess()).isEqualTo(Duration.ofMinutes(15));
            assertThat(properties.maximumSnapshots()).isEqualTo(500L);
            assertThat(properties.maximumDataElements()).isEqualTo(5_000L);
        });
    }

    /**
     * Given 运维关闭本地缓存，When 连续读取同一快照，Then Spring 装配出的缓存每次都执行回源加载器。
     */
    @Test
    void shouldBypassLocalCacheWhenDisabledByExternalProperty() {
        contextRunner.withPropertyValues(PREFIX + ".enabled=false").run(context -> {
            // Given
            ProbabilitySnapshotLocalCache cache = context.getBean(ProbabilitySnapshotLocalCache.class);
            AtomicInteger loadCount = new AtomicInteger();

            // When
            int first = cache.getRateRange(SNAPSHOT_KEY, loadCount::incrementAndGet);
            int second = cache.getRateRange(SNAPSHOT_KEY, loadCount::incrementAndGet);

            // Then
            assertThat(first).isEqualTo(1);
            assertThat(second).isEqualTo(2);
            assertThat(loadCount).hasValue(2);
        });
    }

    /**
     * Given 外部配置提供非法容量，When Spring 创建上下文，Then 启动失败并保留配置校验异常。
     */
    @Test
    void shouldFailContextWhenCacheCapacityIsInvalid() {
        contextRunner.withPropertyValues(PREFIX + ".maximum-data-elements=0").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
            assertThat(context.getStartupFailure()).hasStackTraceContaining(
                    "maximumDataElements must be greater than zero");
        });
    }
}
