package com.lavyoung.marketforge.infrastructure.persistent.repository.cache;

import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AliasProbabilityTable;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityRange;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.ProbabilitySnapshotKey;
import com.lavyoung.marketforge.infrastructure.persistent.repository.config.ProbabilitySnapshotLocalCacheProperties;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证概率快照本地缓存向 Micrometer 暴露的低基数运行指标。
 *
 * <p>测试通过缓存公开行为产生命中、未命中和加载事件，再从指标仓库读取
 * 标准 {@code cache.*} 指标，避免依赖 Caffeine 内部统计实现。</p>
 */
class ProbabilitySnapshotLocalCacheMetricsTest {

    private static final ProbabilitySnapshotKey SNAPSHOT_KEY = new ProbabilitySnapshotKey("100001", 7L);

    private ProbabilitySnapshotLocalCache localCache;
    private SimpleMeterRegistry meterRegistry;

    /**
     * Given 启用统计的本地缓存和独立指标仓库，When 初始化测试，Then 绑定四类缓存指标。
     */
    @BeforeEach
    void setUp() {
        ProbabilitySnapshotLocalCacheProperties properties = new ProbabilitySnapshotLocalCacheProperties(
                true, Duration.ofMinutes(30), 1_000L, 10_000L);
        localCache = new ProbabilitySnapshotLocalCache(properties);
        meterRegistry = new SimpleMeterRegistry();
        new ProbabilitySnapshotLocalCacheMetrics(localCache).bindTo(meterRegistry);
    }

    /**
     * 关闭指标仓库，释放测试持有的 Meter 资源。
     */
    @AfterEach
    void tearDown() {
        meterRegistry.close();
    }

    /**
     * Given 指标绑定器已经注册，When 查询缓存容量指标，Then 四种快照类型各有一组低基数指标。
     */
    @Test
    void shouldRegisterMetricsForEverySnapshotType() {
        // When
        Set<String> snapshotTypes = meterRegistry.find("cache.size")
                .tag("cache", ProbabilitySnapshotLocalCacheMetrics.CACHE_NAME)
                .meters()
                .stream()
                .map(Meter::getId)
                .map(id -> id.getTag(ProbabilitySnapshotLocalCacheMetrics.SNAPSHOT_TYPE_TAG))
                .collect(Collectors.toSet());

        // Then
        assertEquals(Set.of("rate_range", "direct_lookup", "binary_search", "alias_method"), snapshotTypes);
    }

    /**
     * Given 四种概率数据尚未进入缓存，When 每种数据连续读取两次，Then 分别记录一次未命中和一次命中。
     */
    @Test
    void shouldRecordHitMissAndSuccessfulLoadForEverySnapshotType() {
        // Given
        AliasProbabilityTable aliasTable = new AliasProbabilityTable(
                List.of(100_011L, 100_012L), new double[]{0.5D, 1D}, new int[]{1, 1});

        // When
        localCache.getRateRange(SNAPSHOT_KEY, () -> 100);
        localCache.getRateRange(SNAPSHOT_KEY, () -> 200);
        localCache.getDirectLookupTable(SNAPSHOT_KEY, () -> Map.of(0, 100_011L));
        localCache.getDirectLookupTable(SNAPSHOT_KEY, Map::of);
        localCache.getProbabilityRanges(
                SNAPSHOT_KEY, () -> List.of(new AwardProbabilityRange(100, 100_011L)));
        localCache.getProbabilityRanges(SNAPSHOT_KEY, List::of);
        localCache.getAliasTable(SNAPSHOT_KEY, () -> Optional.of(aliasTable));
        localCache.getAliasTable(SNAPSHOT_KEY, Optional::empty);

        // Then
        for (String snapshotType : List.of("rate_range", "direct_lookup", "binary_search", "alias_method")) {
            assertEquals(1D, getCount(snapshotType, "miss"));
            assertEquals(1D, getCount(snapshotType, "hit"));
            assertEquals(1D, getLoadCount(snapshotType));
            assertEquals(1D, getCacheSize(snapshotType));
        }
    }

    /**
     * Given 指标已产生，When 检查 Meter 标签，Then 不包含策略、装配键或快照版本等高基数信息。
     */
    @Test
    void shouldNotExposeHighCardinalitySnapshotIdentityTags() {
        // Given
        localCache.getRateRange(SNAPSHOT_KEY, () -> 100);

        // When & Then
        meterRegistry.getMeters().stream()
                .map(Meter::getId)
                .filter(id -> ProbabilitySnapshotLocalCacheMetrics.CACHE_NAME.equals(id.getTag("cache")))
                .forEach(id -> {
                    assertNull(id.getTag("assembleKey"));
                    assertNull(id.getTag("strategyId"));
                    assertNull(id.getTag("version"));
                });
    }

    /**
     * Given 构造参数或指标仓库为空，When 创建或绑定指标，Then 立即暴露编程错误。
     */
    @Test
    void shouldRejectNullCacheAndMeterRegistry() {
        assertThrows(NullPointerException.class, () -> new ProbabilitySnapshotLocalCacheMetrics(null));
        assertThrows(NullPointerException.class,
                () -> new ProbabilitySnapshotLocalCacheMetrics(localCache).bindTo(null));
    }

    private double getCount(String snapshotType, String result) {
        return meterRegistry.get("cache.gets")
                .tags("cache", ProbabilitySnapshotLocalCacheMetrics.CACHE_NAME)
                .tags(ProbabilitySnapshotLocalCacheMetrics.SNAPSHOT_TYPE_TAG, snapshotType)
                .tag("result", result)
                .functionCounter()
                .count();
    }

    private double getLoadCount(String snapshotType) {
        return meterRegistry.get("cache.puts")
                .tags("cache", ProbabilitySnapshotLocalCacheMetrics.CACHE_NAME)
                .tags(ProbabilitySnapshotLocalCacheMetrics.SNAPSHOT_TYPE_TAG, snapshotType)
                .functionCounter()
                .count();
    }

    private double getCacheSize(String snapshotType) {
        return meterRegistry.get("cache.size")
                .tags("cache", ProbabilitySnapshotLocalCacheMetrics.CACHE_NAME)
                .tags(ProbabilitySnapshotLocalCacheMetrics.SNAPSHOT_TYPE_TAG, snapshotType)
                .gauge()
                .value();
    }
}
