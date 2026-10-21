package com.lavyoung.marketforge.infrastructure.persistent.repository.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证概率快照本地缓存配置的默认值、外部绑定和容量保护约束。
 *
 * <p>缓存配置直接决定 JVM 中最多保留多少概率数据。测试需要保证缺省配置
 * 具备明确上限，同时阻止零容量、负容量和无效过期时间进入运行阶段。</p>
 */
class ProbabilitySnapshotLocalCachePropertiesTest {

    private static final String PREFIX = "market-forge.strategy.snapshot.local-cache";

    /**
     * Given 只配置最大快照数量，When Spring Boot 绑定配置，Then 其余字段使用安全默认值。
     */
    @Test
    void shouldApplyDefaultsWhenPropertiesArePartiallyConfigured() {
        // Given
        var source = new MapConfigurationPropertySource(Map.of(PREFIX + ".maximum-snapshots", "20000"));

        // When
        ProbabilitySnapshotLocalCacheProperties properties = bind(source);

        // Then
        assertAll(
                () -> assertTrue(properties.enabled()),
                () -> assertEquals(Duration.ofMinutes(30), properties.expireAfterAccess()),
                () -> assertEquals(20_000L, properties.maximumSnapshots()),
                () -> assertEquals(100_000L, properties.maximumDataElements())
        );
    }

    /**
     * Given 部署环境提供完整缓存参数，When Spring Boot 绑定配置，Then 使用全部外部配置值。
     */
    @Test
    void shouldBindConfiguredLocalCacheProperties() {
        // Given
        var source = new MapConfigurationPropertySource(Map.of(
                PREFIX + ".enabled", "false",
                PREFIX + ".expire-after-access", "15m",
                PREFIX + ".maximum-snapshots", "5000",
                PREFIX + ".maximum-data-elements", "50000"));

        // When
        ProbabilitySnapshotLocalCacheProperties properties = bind(source);

        // Then
        assertAll(
                () -> assertFalse(properties.enabled()),
                () -> assertEquals(Duration.ofMinutes(15), properties.expireAfterAccess()),
                () -> assertEquals(5_000L, properties.maximumSnapshots()),
                () -> assertEquals(50_000L, properties.maximumDataElements())
        );
    }

    /**
     * Given 过期时间或容量不合法，When 创建配置对象，Then 在缓存初始化前拒绝启动。
     */
    @Test
    void shouldRejectInvalidLocalCacheBoundaries() {
        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> new ProbabilitySnapshotLocalCacheProperties(true, null, 1L, 1L)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ProbabilitySnapshotLocalCacheProperties(true, Duration.ZERO, 1L, 1L)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ProbabilitySnapshotLocalCacheProperties(true, Duration.ofMinutes(1), 0L, 1L)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> new ProbabilitySnapshotLocalCacheProperties(true, Duration.ofMinutes(1), 1L, -1L))
        );
    }

    private ProbabilitySnapshotLocalCacheProperties bind(MapConfigurationPropertySource source) {
        return new Binder(source).bind(PREFIX, Bindable.of(ProbabilitySnapshotLocalCacheProperties.class))
                .orElseThrow(() -> new IllegalStateException("probability snapshot local cache properties not bound"));
    }
}
