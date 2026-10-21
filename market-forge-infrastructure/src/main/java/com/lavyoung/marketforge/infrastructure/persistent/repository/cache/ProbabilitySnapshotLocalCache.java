package com.lavyoung.marketforge.infrastructure.persistent.repository.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AliasProbabilityTable;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityRange;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.ProbabilitySnapshotKey;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.ProbabilitySnapshotPointer;
import com.lavyoung.marketforge.infrastructure.persistent.repository.config.ProbabilitySnapshotLocalCacheProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * 带版本号的概率快照 JVM 本地缓存。
 *
 * <p>缓存键包含装配键和版本，因此新旧快照可以在宽限期内同时存在。
 * 当前生效指针不在本地缓存，每次抽奖仍通过 Redis 获取当前版本。</p>
 *
 * <p>Caffeine 的原子加载避免同一版本首次访问时产生 JVM 内缓存击穿；
 * 基于元素数量的权重淘汰避免大型概率表无限占用堆内存。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@Component
public class ProbabilitySnapshotLocalCache {

    private final boolean enabled;
    private final Cache<ProbabilitySnapshotKey, Integer> rateRanges;
    private final Cache<ProbabilitySnapshotKey, Map<Integer, Long>> directLookupTables;
    private final Cache<ProbabilitySnapshotKey, List<AwardProbabilityRange>> probabilityRanges;
    private final Cache<ProbabilitySnapshotKey, AliasProbabilityTable> aliasTables;

    /**
     * 按外部配置创建有界本地缓存。
     *
     * @param properties 本地缓存容量和生命周期配置
     * @throws NullPointerException 配置为空时抛出
     */
    public ProbabilitySnapshotLocalCache(ProbabilitySnapshotLocalCacheProperties properties) {
        ProbabilitySnapshotLocalCacheProperties config =
                Objects.requireNonNull(properties, "properties must not be null");
        enabled = config.enabled();
        rateRanges = Caffeine.<ProbabilitySnapshotKey, Integer>newBuilder()
                .maximumSize(config.maximumSnapshots())
                .expireAfterAccess(config.expireAfterAccess())
                .build();
        directLookupTables = buildWeightedCache(config, Map::size);
        probabilityRanges = buildWeightedCache(config, List::size);
        aliasTables = buildWeightedCache(config, AliasProbabilityTable::size);
    }

    /**
     * 获取概率空间总权重，缓存未命中时原子执行加载器。
     *
     * @param snapshotKey 快照键
     * @param loader      Redis 加载器
     * @return 总权重；不存在时返回零
     */
    public int getRateRange(ProbabilitySnapshotKey snapshotKey, IntSupplier loader) {
        Integer value = getOrLoad(rateRanges, snapshotKey, loader::getAsInt, candidate -> candidate > 0);
        return value == null ? 0 : value;
    }

    /**
     * 获取完整直接查找表。
     *
     * @param snapshotKey 快照键
     * @param loader      Redis 加载器
     * @return 不可变查找表；不存在时返回空 Map
     */
    public Map<Integer, Long> getDirectLookupTable(
            ProbabilitySnapshotKey snapshotKey, Supplier<Map<Integer, Long>> loader) {
        Map<Integer, Long> value = getOrLoad(directLookupTables, snapshotKey,
                () -> Map.copyOf(Objects.requireNonNull(loader.get(), "loaded table must not be null")),
                candidate -> !candidate.isEmpty());
        return value == null ? Map.of() : value;
    }

    /**
     * 获取二分查找使用的累计概率区间。
     *
     * @param snapshotKey 快照键
     * @param loader      Redis 加载器
     * @return 不可变区间列表；不存在时返回空列表
     */
    public List<AwardProbabilityRange> getProbabilityRanges(
            ProbabilitySnapshotKey snapshotKey, Supplier<List<AwardProbabilityRange>> loader) {
        List<AwardProbabilityRange> value = getOrLoad(probabilityRanges, snapshotKey,
                () -> List.copyOf(Objects.requireNonNull(loader.get(), "loaded ranges must not be null")),
                candidate -> !candidate.isEmpty());
        return value == null ? List.of() : value;
    }

    /**
     * 获取 Alias Method 概率表。
     *
     * @param snapshotKey 快照键
     * @param loader      Redis 加载器
     * @return Alias 概率表；不存在时返回空
     */
    public Optional<AliasProbabilityTable> getAliasTable(
            ProbabilitySnapshotKey snapshotKey, Supplier<Optional<AliasProbabilityTable>> loader) {
        AliasProbabilityTable value = getOrLoad(aliasTables, snapshotKey,
                () -> Objects.requireNonNull(loader.get(), "loaded alias result must not be null").orElse(null),
                Objects::nonNull);
        return Optional.ofNullable(value);
    }

    /**
     * 立即移除指定退役快照在当前 JVM 中的数据。
     *
     * @param pointer 退役快照指针
     * @throws NullPointerException 指针为空时抛出
     */
    public void invalidate(ProbabilitySnapshotPointer pointer) {
        ProbabilitySnapshotPointer validatedPointer =
                Objects.requireNonNull(pointer, "pointer must not be null");
        ProbabilitySnapshotKey snapshotKey = validatedPointer.snapshotKey();
        rateRanges.invalidate(snapshotKey);

        switch (validatedPointer.algorithmType()) {
            case DIRECT_LOOKUP -> directLookupTables.invalidate(snapshotKey);
            case BINARY_SEARCH -> probabilityRanges.invalidate(snapshotKey);
            case ALIAS_METHOD -> aliasTables.invalidate(snapshotKey);
        }
    }

    private <V> Cache<ProbabilitySnapshotKey, V> buildWeightedCache(
            ProbabilitySnapshotLocalCacheProperties properties, ToIntFunction<V> weigher) {
        return Caffeine.<ProbabilitySnapshotKey, V>newBuilder()
                .maximumWeight(properties.maximumDataElements())
                .weigher((ProbabilitySnapshotKey key, V value) -> Math.max(1, weigher.applyAsInt(value)))
                .expireAfterAccess(properties.expireAfterAccess())
                .build();
    }

    private <V> V getOrLoad(Cache<ProbabilitySnapshotKey, V> cache, ProbabilitySnapshotKey snapshotKey,
                            Supplier<V> loader, Predicate<V> cacheable) {
        Objects.requireNonNull(snapshotKey, "snapshotKey must not be null");
        Objects.requireNonNull(loader, "loader must not be null");
        if (!enabled) {
            return loader.get();
        }
        return cache.get(snapshotKey, ignored -> {
            V loaded = loader.get();
            return cacheable.test(loaded) ? loaded : null;
        });
    }
}