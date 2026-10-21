package com.lavyoung.marketforge.infrastructure.persistent.repository.cache;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 概率快照本地缓存的 Micrometer 指标绑定器。
 *
 * <p>四种缓存使用统一缓存名称，通过低基数的 {@code snapshot.type}
 * 标签区分数据结构。指标中不得加入策略标识、装配键或版本号，避免策略数量
 * 增长时创建不可控数量的时间序列。</p>
 *
 * <p>应用引入 Actuator 后，Spring Boot 会自动发现 {@link MeterBinder}，
 * 并将命中、未命中、加载、容量和淘汰数据注册到全局指标仓库。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@Component
public class ProbabilitySnapshotLocalCacheMetrics implements MeterBinder {

    static final String CACHE_NAME = "market-forge.probability.snapshot";
    static final String SNAPSHOT_TYPE_TAG = "snapshot.type";

    private final ProbabilitySnapshotLocalCache localCache;

    /**
     * 创建概率快照缓存指标绑定器。
     *
     * @param localCache 需要采集运行指标的本地缓存
     * @throws NullPointerException 本地缓存为空时抛出
     */
    public ProbabilitySnapshotLocalCacheMetrics(ProbabilitySnapshotLocalCache localCache) {
        this.localCache = Objects.requireNonNull(localCache, "localCache must not be null");
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        Objects.requireNonNull(registry, "registry must not be null");

        CaffeineCacheMetrics.monitor(
                registry, localCache.rateRangeCache(), CACHE_NAME,
                SNAPSHOT_TYPE_TAG, "rate_range");

        CaffeineCacheMetrics.monitor(
                registry, localCache.directLookupTableCache(), CACHE_NAME,
                SNAPSHOT_TYPE_TAG, "direct_lookup");

        CaffeineCacheMetrics.monitor(
                registry, localCache.probabilityRangeCache(), CACHE_NAME,
                SNAPSHOT_TYPE_TAG, "binary_search");

        CaffeineCacheMetrics.monitor(
                registry, localCache.aliasTableCache(), CACHE_NAME,
                SNAPSHOT_TYPE_TAG, "alias_method");
    }
}