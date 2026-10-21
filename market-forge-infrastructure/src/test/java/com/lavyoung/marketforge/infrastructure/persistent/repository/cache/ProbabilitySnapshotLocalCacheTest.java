package com.lavyoung.marketforge.infrastructure.persistent.repository.cache;

import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilityAlgorithmType;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AliasProbabilityTable;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityRange;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.ProbabilitySnapshotKey;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.ProbabilitySnapshotPointer;
import com.lavyoung.marketforge.infrastructure.persistent.repository.config.ProbabilitySnapshotLocalCacheProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证概率快照 JVM 本地缓存的命中、旁路、容量、不可变性和并发加载语义。
 *
 * <p>这些测试不依赖 Redis，加载器代表基础设施从 Redis 读取快照的动作。
 * 加载次数用于判断同一版本是否被重复访问，进而验证本地缓存是否真正减少
 * 网络读取，以及快照退役后是否能够清除当前 JVM 中的旧数据。</p>
 */
class ProbabilitySnapshotLocalCacheTest {

    private static final ProbabilitySnapshotKey SNAPSHOT_KEY = new ProbabilitySnapshotKey("100001", 7L);

    /**
     * Given 缓存已启用且概率总权重有效，When 连续读取同一快照，Then 只执行一次底层加载。
     */
    @Test
    void shouldCachePositiveRateRange() {
        // Given
        ProbabilitySnapshotLocalCache cache = enabledCache();
        AtomicInteger loadCount = new AtomicInteger();

        // When
        int first = cache.getRateRange(SNAPSHOT_KEY, () -> {
            loadCount.incrementAndGet();
            return 100;
        });
        int second = cache.getRateRange(SNAPSHOT_KEY, () -> {
            loadCount.incrementAndGet();
            return 200;
        });

        // Then
        assertEquals(100, first);
        assertEquals(100, second);
        assertEquals(1, loadCount.get());
    }

    /**
     * Given 本地缓存已关闭，When 连续读取同一快照，Then 每次都旁路缓存并调用加载器。
     */
    @Test
    void shouldBypassCacheWhenDisabled() {
        // Given
        ProbabilitySnapshotLocalCache cache = new ProbabilitySnapshotLocalCache(properties(false, 10_000L));
        AtomicInteger loadCount = new AtomicInteger();

        // When
        int first = cache.getRateRange(SNAPSHOT_KEY, loadCount::incrementAndGet);
        int second = cache.getRateRange(SNAPSHOT_KEY, loadCount::incrementAndGet);

        // Then
        assertEquals(1, first);
        assertEquals(2, second);
        assertEquals(2, loadCount.get());
    }

    /**
     * Given Redis 暂时没有完整快照，When 连续读取无效结果，Then 不缓存零值和空数据，允许后续重试恢复。
     */
    @Test
    void shouldNotCacheEmptySnapshotData() {
        // Given
        ProbabilitySnapshotLocalCache cache = enabledCache();
        AtomicInteger rateRangeLoadCount = new AtomicInteger();
        AtomicInteger tableLoadCount = new AtomicInteger();
        AtomicInteger rangeLoadCount = new AtomicInteger();
        AtomicInteger aliasLoadCount = new AtomicInteger();

        // When
        cache.getRateRange(SNAPSHOT_KEY, () -> {
            rateRangeLoadCount.incrementAndGet();
            return 0;
        });
        cache.getRateRange(SNAPSHOT_KEY, () -> {
            rateRangeLoadCount.incrementAndGet();
            return 0;
        });
        cache.getDirectLookupTable(SNAPSHOT_KEY, () -> {
            tableLoadCount.incrementAndGet();
            return Map.of();
        });
        cache.getDirectLookupTable(SNAPSHOT_KEY, () -> {
            tableLoadCount.incrementAndGet();
            return Map.of();
        });
        cache.getProbabilityRanges(SNAPSHOT_KEY, () -> {
            rangeLoadCount.incrementAndGet();
            return List.of();
        });
        cache.getProbabilityRanges(SNAPSHOT_KEY, () -> {
            rangeLoadCount.incrementAndGet();
            return List.of();
        });
        cache.getAliasTable(SNAPSHOT_KEY, () -> {
            aliasLoadCount.incrementAndGet();
            return Optional.empty();
        });
        cache.getAliasTable(SNAPSHOT_KEY, () -> {
            aliasLoadCount.incrementAndGet();
            return Optional.empty();
        });

        // Then
        assertEquals(2, rateRangeLoadCount.get());
        assertEquals(2, tableLoadCount.get());
        assertEquals(2, rangeLoadCount.get());
        assertEquals(2, aliasLoadCount.get());
    }

    /**
     * Given 加载器返回可变直接查找表，When 数据进入缓存，Then 缓存保存独立的不可变副本。
     */
    @Test
    void shouldCacheImmutableDirectLookupTableCopy() {
        // Given
        ProbabilitySnapshotLocalCache cache = enabledCache();
        Map<Integer, Long> loadedTable = new HashMap<>();
        loadedTable.put(0, 100_011L);

        // When
        Map<Integer, Long> cachedTable = cache.getDirectLookupTable(SNAPSHOT_KEY, () -> loadedTable);
        loadedTable.put(1, 100_012L);
        Map<Integer, Long> secondRead = cache.getDirectLookupTable(SNAPSHOT_KEY, Map::of);

        // Then
        assertEquals(Map.of(0, 100_011L), cachedTable);
        assertEquals(cachedTable, secondRead);
        assertThrows(UnsupportedOperationException.class, () -> cachedTable.put(2, 100_013L));
    }

    /**
     * Given 加载器返回可变累计区间列表，When 数据进入缓存，Then 后续修改不会污染快照。
     */
    @Test
    void shouldCacheImmutableProbabilityRangeCopy() {
        // Given
        ProbabilitySnapshotLocalCache cache = enabledCache();
        List<AwardProbabilityRange> loadedRanges = new ArrayList<>();
        loadedRanges.add(new AwardProbabilityRange(60, 100_011L));

        // When
        List<AwardProbabilityRange> cachedRanges = cache.getProbabilityRanges(SNAPSHOT_KEY, () -> loadedRanges);
        loadedRanges.add(new AwardProbabilityRange(100, 100_012L));
        List<AwardProbabilityRange> secondRead = cache.getProbabilityRanges(SNAPSHOT_KEY, List::of);

        // Then
        assertEquals(List.of(new AwardProbabilityRange(60, 100_011L)), cachedRanges);
        assertEquals(cachedRanges, secondRead);
        assertThrows(UnsupportedOperationException.class,
                () -> cachedRanges.add(new AwardProbabilityRange(100, 100_012L)));
    }

    /**
     * Given Alias 快照已经加载，When 再次读取相同版本，Then 返回原快照且不重复调用加载器。
     */
    @Test
    void shouldCacheAliasProbabilityTable() {
        // Given
        ProbabilitySnapshotLocalCache cache = enabledCache();
        AliasProbabilityTable table = aliasTable();
        AtomicInteger loadCount = new AtomicInteger();

        // When
        Optional<AliasProbabilityTable> first = cache.getAliasTable(SNAPSHOT_KEY, () -> {
            loadCount.incrementAndGet();
            return Optional.of(table);
        });
        Optional<AliasProbabilityTable> second = cache.getAliasTable(SNAPSHOT_KEY, () -> {
            loadCount.incrementAndGet();
            return Optional.empty();
        });

        // Then
        assertTrue(first.isPresent());
        assertTrue(second.isPresent());
        assertEquals(table, second.orElseThrow());
        assertEquals(1, loadCount.get());
    }

    /**
     * Given 同一快照被多个请求同时首次访问，When 加载尚未完成，Then Caffeine 只允许一个线程执行加载器。
     *
     * @throws Exception 并发任务等待超时或执行失败时抛出
     */
    @Test
    void shouldLoadSameSnapshotOnlyOnceDuringConcurrentAccess() throws Exception {
        // Given
        int concurrency = 8;
        ProbabilitySnapshotLocalCache cache = enabledCache();
        AtomicInteger loadCount = new AtomicInteger();
        CountDownLatch startGate = new CountDownLatch(1);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                concurrency, concurrency, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(concurrency), new ThreadPoolExecutor.AbortPolicy());

        try {
            List<CompletableFuture<Integer>> futures = new ArrayList<>();
            for (int index = 0; index < concurrency; index++) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    await(startGate);
                    return cache.getRateRange(SNAPSHOT_KEY, () -> {
                        loadCount.incrementAndGet();
                        sleepBriefly();
                        return 100;
                    });
                }, executor));
            }

            // When
            startGate.countDown();
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get(5, TimeUnit.SECONDS);

            // Then
            assertTrue(futures.stream().map(CompletableFuture::join).allMatch(value -> value == 100));
            assertEquals(1, loadCount.get());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    /**
     * Given 三种算法数据均已缓存，When 依次退役各算法快照，Then 只移除总权重和对应算法的数据。
     */
    @Test
    void shouldInvalidateOnlyRetiredAlgorithmSnapshot() {
        // Given
        ProbabilitySnapshotLocalCache cache = enabledCache();
        AtomicInteger rateRangeLoadCount = new AtomicInteger();
        AtomicInteger directLoadCount = new AtomicInteger();
        AtomicInteger rangeLoadCount = new AtomicInteger();
        AtomicInteger aliasLoadCount = new AtomicInteger();
        loadAllSnapshotData(cache, rateRangeLoadCount, directLoadCount, rangeLoadCount, aliasLoadCount);

        // When
        cache.invalidate(new ProbabilitySnapshotPointer("100001", ProbabilityAlgorithmType.DIRECT_LOOKUP, 7L));
        cache.getRateRange(SNAPSHOT_KEY, () -> incrementAndReturn(rateRangeLoadCount, 100));
        cache.getDirectLookupTable(SNAPSHOT_KEY,
                () -> incrementAndReturn(directLoadCount, Map.of(0, 100_011L)));
        cache.getProbabilityRanges(SNAPSHOT_KEY,
                () -> incrementAndReturn(rangeLoadCount, List.of(new AwardProbabilityRange(100, 100_011L))));
        cache.getAliasTable(SNAPSHOT_KEY, () -> incrementAndReturn(aliasLoadCount, Optional.of(aliasTable())));

        cache.invalidate(new ProbabilitySnapshotPointer("100001", ProbabilityAlgorithmType.BINARY_SEARCH, 7L));
        cache.getProbabilityRanges(SNAPSHOT_KEY,
                () -> incrementAndReturn(rangeLoadCount, List.of(new AwardProbabilityRange(100, 100_011L))));

        cache.invalidate(new ProbabilitySnapshotPointer("100001", ProbabilityAlgorithmType.ALIAS_METHOD, 7L));
        cache.getAliasTable(SNAPSHOT_KEY, () -> incrementAndReturn(aliasLoadCount, Optional.of(aliasTable())));

        // Then
        assertEquals(2, rateRangeLoadCount.get());
        assertEquals(2, directLoadCount.get());
        assertEquals(2, rangeLoadCount.get());
        assertEquals(2, aliasLoadCount.get());
    }

    /**
     * Given 单个直接查找表的元素数量超过缓存总权重，When 重复读取，Then 超重条目不会长期驻留。
     */
    @Test
    void shouldNotRetainEntryLargerThanMaximumWeight() {
        // Given
        ProbabilitySnapshotLocalCache cache = new ProbabilitySnapshotLocalCache(properties(true, 1L));
        AtomicInteger loadCount = new AtomicInteger();

        // When
        for (long version = 1L; version <= 100L; version++) {
            cache.getDirectLookupTable(new ProbabilitySnapshotKey("100001", version),
                    () -> incrementAndReturn(loadCount, Map.of(0, 100_011L, 1, 100_012L)));
        }
        cache.getDirectLookupTable(SNAPSHOT_KEY,
                () -> incrementAndReturn(loadCount, Map.of(0, 100_011L, 1, 100_012L)));

        // Then
        assertEquals(101, loadCount.get());
    }

    /**
     * Given 调用参数或加载结果违反缓存契约，When 执行读取或失效，Then 立即暴露编程错误。
     */
    @Test
    void shouldRejectNullArgumentsAndLoadedValues() {
        // Given
        ProbabilitySnapshotLocalCache cache = enabledCache();

        // When & Then
        assertThrows(NullPointerException.class, () -> cache.getRateRange(null, () -> 100));
        assertThrows(NullPointerException.class, () -> cache.getRateRange(SNAPSHOT_KEY, null));
        assertThrows(NullPointerException.class, () -> cache.getDirectLookupTable(SNAPSHOT_KEY, () -> null));
        assertThrows(NullPointerException.class, () -> cache.getProbabilityRanges(SNAPSHOT_KEY, () -> null));
        assertThrows(NullPointerException.class, () -> cache.getAliasTable(SNAPSHOT_KEY, () -> null));
        assertThrows(NullPointerException.class, () -> cache.invalidate(null));
    }

    private ProbabilitySnapshotLocalCache enabledCache() {
        return new ProbabilitySnapshotLocalCache(properties(true, 10_000L));
    }

    private ProbabilitySnapshotLocalCacheProperties properties(boolean enabled, long maximumDataElements) {
        return new ProbabilitySnapshotLocalCacheProperties(enabled, Duration.ofMinutes(30), 1_000L,
                maximumDataElements);
    }

    private AliasProbabilityTable aliasTable() {
        return new AliasProbabilityTable(List.of(100_011L, 100_012L), new double[]{0.5D, 1D}, new int[]{1, 1});
    }

    private void loadAllSnapshotData(ProbabilitySnapshotLocalCache cache, AtomicInteger rateRangeLoadCount,
                                     AtomicInteger directLoadCount, AtomicInteger rangeLoadCount,
                                     AtomicInteger aliasLoadCount) {
        cache.getRateRange(SNAPSHOT_KEY, () -> incrementAndReturn(rateRangeLoadCount, 100));
        cache.getDirectLookupTable(SNAPSHOT_KEY,
                () -> incrementAndReturn(directLoadCount, Map.of(0, 100_011L)));
        cache.getProbabilityRanges(SNAPSHOT_KEY,
                () -> incrementAndReturn(rangeLoadCount, List.of(new AwardProbabilityRange(100, 100_011L))));
        cache.getAliasTable(SNAPSHOT_KEY, () -> incrementAndReturn(aliasLoadCount, Optional.of(aliasTable())));
    }

    private int incrementAndReturn(AtomicInteger counter, int value) {
        counter.incrementAndGet();
        return value;
    }

    private <T> T incrementAndReturn(AtomicInteger counter, T value) {
        counter.incrementAndGet();
        return value;
    }

    private void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for concurrent cache test", exception);
        }
    }

    private void sleepBriefly() {
        try {
            TimeUnit.MILLISECONDS.sleep(50L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while simulating snapshot loading", exception);
        }
    }
}
