package com.lavyoung.marketforge.infrastructure.persistent.repository.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证概率快照退役配置的默认值、外部配置绑定和合法性约束。
 *
 * <p>这些约束决定旧快照在 Redis 中保留多久。默认值必须足够覆盖在途抽奖请求，
 * 同时应允许部署环境按实际请求耗时调整；零值和负值会破坏宽限期语义，必须在启动阶段拒绝。</p>
 */
class ProbabilitySnapshotRetentionPropertiesTest {

    /**
     * Given 未配置退役宽限期，When 创建配置对象，Then 使用十分钟默认值。
     */
    @Test
    void shouldUseDefaultGracePeriodWhenValueIsAbsent() {
        // Given & When
        ProbabilitySnapshotRetentionProperties properties = new ProbabilitySnapshotRetentionProperties(null);

        // Then
        assertEquals(Duration.ofMinutes(10), properties.retirementGracePeriod());
    }

    /**
     * Given 部署环境配置十五分钟宽限期，When Spring Boot 绑定配置，Then 使用外部配置值。
     */
    @Test
    void shouldBindConfiguredGracePeriod() {
        // Given
        var source = new MapConfigurationPropertySource(Map.of(
                "market-forge.strategy.snapshot.retirement-grace-period", "15m"));

        // When
        ProbabilitySnapshotRetentionProperties properties = new Binder(source)
                .bind("market-forge.strategy.snapshot", Bindable.of(ProbabilitySnapshotRetentionProperties.class))
                .orElseThrow(() -> new IllegalStateException("probability snapshot retention properties not bound"));

        // Then
        assertEquals(Duration.ofMinutes(15), properties.retirementGracePeriod());
    }

    /**
     * Given 宽限期为零或负数，When 创建配置对象，Then 拒绝无法保护在途请求的配置。
     */
    @Test
    void shouldRejectNonPositiveGracePeriod() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProbabilitySnapshotRetentionProperties(Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new ProbabilitySnapshotRetentionProperties(Duration.ofSeconds(-1)));
    }
}
