package com.lavyoung.marketforge;

import com.lavyoung.marketforge.app.MarketForgeApplication;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyArmory;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyDispatch;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilityAlgorithmType;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.ProbabilitySnapshotPointer;
import com.lavyoung.marketforge.types.common.Constants;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 使用开发环境 MySQL、Redis 和 Micrometer 验证版本化概率快照的完整运行链路。
 *
 * <p>测试只操作初始化 SQL 中专门用于演示的策略 {@code 900901001}。它先装配
 * 一个新快照，验证首次抽奖产生本地缓存未命中、后续抽奖产生缓存命中；然后
 * 再次装配并确认版本递增，新版本首次访问必须重新从 Redis 加载。</p>
 *
 * <p>只有显式设置 {@code RUN_DEV_INTEGRATION_TESTS=true} 时才执行，避免常规
 * 单元测试连接开发环境基础设施。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
@SpringBootTest(classes = MarketForgeApplication.class)
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "RUN_DEV_INTEGRATION_TESTS", matches = "true")
@Slf4j
public class StrategyArmoryDispatchDevIntegrationTest {

    private static final long STRATEGY_ID = 900_901_001L;
    private static final int DRAW_COUNT = 1_000;
    private static final String CACHE_NAME = "market-forge.probability.snapshot";
    private static final String SNAPSHOT_TYPE_TAG = "snapshot.type";
    private static final Set<Long> ASSEMBLED_AWARD_IDS = Set.of(
            900_901_011L, 900_901_012L, 900_901_013L, 900_901_014L, 900_901_015L,
            900_901_016L, 900_901_017L, 900_901_018L, 900_901_019L
    );

    private final RedissonClient redissonClient;
    private final IStrategyArmory strategyArmory;
    private final IStrategyDispatch strategyDispatch;
    private final IStrategyRepository strategyRepository;
    private final MeterRegistry meterRegistry;

    /**
     * 创建使用开发环境真实基础设施和应用指标仓库的策略装配集成测试。
     *
     * @param redissonClient     开发环境 Redis 客户端
     * @param strategyArmory     策略装配领域服务
     * @param strategyDispatch   策略派发领域服务
     * @param strategyRepository 策略快照指针仓储
     * @param meterRegistry      应用 Micrometer 指标仓库
     */
    @Autowired
    public StrategyArmoryDispatchDevIntegrationTest(RedissonClient redissonClient, IStrategyArmory strategyArmory,
                                                    IStrategyDispatch strategyDispatch,
                                                    IStrategyRepository strategyRepository,
                                                    MeterRegistry meterRegistry) {
        this.redissonClient = redissonClient;
        this.strategyArmory = strategyArmory;
        this.strategyDispatch = strategyDispatch;
        this.strategyRepository = strategyRepository;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 验证真实装配、JVM 缓存预热、连续命中和新版本重新加载的完整闭环。
     */
    @Test
    void shouldExposeCacheMissHitAndReloadMetricsAcrossSnapshotVersions() {
        // Given
        clearAssembledStrategyCache();
        strategyArmory.assembleLotteryStrategy(STRATEGY_ID);
        ProbabilitySnapshotPointer firstPointer = getRequiredPointer();
        List<String> snapshotTypes = snapshotTypes(firstPointer.algorithmType());
        Map<String, Double> missesBeforeFirstDraw = cacheGetCounts(snapshotTypes, "miss");
        Map<String, Double> hitsBeforeFirstDraw = cacheGetCounts(snapshotTypes, "hit");

        // When：首次访问新版本，需要从 Redis 加载概率数据。
        long firstAwardId = strategyDispatch.getRandomAwardId(STRATEGY_ID);
        Map<String, Double> missesAfterFirstDraw = cacheGetCounts(snapshotTypes, "miss");

        // Then：每个参与当前算法的数据缓存都产生一次未命中。
        assertTrue(ASSEMBLED_AWARD_IDS.contains(firstAwardId), "首次抽奖返回了未配置的奖品: " + firstAwardId);
        assertMetricIncrease(missesBeforeFirstDraw, missesAfterFirstDraw, 1D);

        // When：继续使用同一版本抽奖，概率数据应全部从 JVM 本地缓存读取。
        Map<Long, Integer> awardCounts = drawAwards(DRAW_COUNT - 1);
        Map<String, Double> hitsAfterWarmDraws = cacheGetCounts(snapshotTypes, "hit");

        // Then：每次抽奖都会命中当前算法所需的各个本地缓存。
        assertFalse(awardCounts.isEmpty());
        assertEquals(DRAW_COUNT - 1, awardCounts.values().stream().mapToInt(Integer::intValue).sum());
        assertMetricIncrease(hitsBeforeFirstDraw, hitsAfterWarmDraws, DRAW_COUNT - 1D);

        // When：重新装配同一策略并首次访问新版本。
        strategyArmory.assembleLotteryStrategy(STRATEGY_ID);
        ProbabilitySnapshotPointer secondPointer = getRequiredPointer();
        Map<String, Double> missesBeforeVersionReload = cacheGetCounts(snapshotTypes, "miss");
        long reloadedAwardId = strategyDispatch.getRandomAwardId(STRATEGY_ID);
        Map<String, Double> missesAfterVersionReload = cacheGetCounts(snapshotTypes, "miss");

        // Then：版本单调递增，新版本不能复用旧版本缓存数据。
        assertEquals(firstPointer.algorithmType(), secondPointer.algorithmType());
        assertTrue(secondPointer.version() > firstPointer.version());
        assertTrue(ASSEMBLED_AWARD_IDS.contains(reloadedAwardId), "新版本抽奖返回了未配置的奖品: " + reloadedAwardId);
        assertMetricIncrease(missesBeforeVersionReload, missesAfterVersionReload, 1D);

        log.info("开发环境概率快照本地缓存验证完成 strategyId={} algorithm={} firstVersion={} secondVersion={} draws={} results={}",
                STRATEGY_ID, firstPointer.algorithmType().getCode(), firstPointer.version(),
                secondPointer.version(), DRAW_COUNT, awardCounts);
    }

    /**
     * 清理测试策略当前指针和版本化概率数据，同时保留单调版本计数器。
     */
    @AfterEach
    void cleanUpAssembledStrategyCache() {
        clearAssembledStrategyCache();
    }

    private Map<Long, Integer> drawAwards(int drawCount) {
        Map<Long, Integer> awardCounts = new HashMap<>();
        for (int index = 0; index < drawCount; index++) {
            long awardId = strategyDispatch.getRandomAwardId(STRATEGY_ID);
            assertTrue(ASSEMBLED_AWARD_IDS.contains(awardId), "返回了未配置的奖品: " + awardId);
            awardCounts.merge(awardId, 1, Integer::sum);
        }
        return awardCounts;
    }

    private ProbabilitySnapshotPointer getRequiredPointer() {
        return strategyRepository.getProbabilitySnapshotPointer(String.valueOf(STRATEGY_ID))
                .orElseThrow(() -> new AssertionError("策略装配后没有发布概率快照指针"));
    }

    private List<String> snapshotTypes(ProbabilityAlgorithmType algorithmType) {
        return switch (algorithmType) {
            case DIRECT_LOOKUP -> List.of("rate_range", "direct_lookup");
            case BINARY_SEARCH -> List.of("rate_range", "binary_search");
            case ALIAS_METHOD -> List.of("alias_method");
        };
    }

    private Map<String, Double> cacheGetCounts(List<String> snapshotTypes, String result) {
        Map<String, Double> counts = new HashMap<>();
        for (String snapshotType : snapshotTypes) {
            double count = meterRegistry.get("cache.gets")
                    .tags("cache", CACHE_NAME)
                    .tags(SNAPSHOT_TYPE_TAG, snapshotType)
                    .tag("result", result)
                    .functionCounter()
                    .count();
            counts.put(snapshotType, count);
        }
        return Map.copyOf(counts);
    }

    private void assertMetricIncrease(Map<String, Double> before, Map<String, Double> after, double expectedIncrease) {
        before.forEach((snapshotType, beforeCount) -> assertEquals(
                expectedIncrease, after.get(snapshotType) - beforeCount, 0.001D,
                "缓存指标增量不符合预期: " + snapshotType));
    }

    private void clearAssembledStrategyCache() {
        clearProbabilitySpaceCache(String.valueOf(STRATEGY_ID));
        clearProbabilitySpaceCache(STRATEGY_ID + Constants.UNDERLINE + "40000");
        clearProbabilitySpaceCache(STRATEGY_ID + Constants.UNDERLINE + "60000");
    }

    private void clearProbabilitySpaceCache(String assembleKey) {
        strategyRepository.getProbabilitySnapshotPointer(assembleKey)
                .ifPresent(strategyRepository::retireProbabilitySnapshot);

        redissonClient.getKeys().delete(
                Constants.RedisKeys.STRATEGY_PROBABILITY_SNAPSHOT_POINTER_KEY + assembleKey,
                Constants.RedisKeys.STRATEGY_PROBABILITY_SNAPSHOT_PUBLISH_LOCK_KEY + assembleKey,
                Constants.RedisKeys.STRATEGY_AWARD_KEY + assembleKey,
                Constants.RedisKeys.STRATEGY_RATE_RANGE_KEY + assembleKey,
                Constants.RedisKeys.STRATEGY_RATE_TABLE_KEY + assembleKey,
                Constants.RedisKeys.STRATEGY_PROBABILITY_RANGE_TABLE_KEY + assembleKey,
                Constants.RedisKeys.STRATEGY_ALIAS_TABLE_KEY + assembleKey,
                Constants.RedisKeys.STRATEGY_PROBABILITY_ALGORITHM_KEY + assembleKey
        );
        redissonClient.getKeys().deleteByPattern(Constants.RedisKeys.STRATEGY_RATE_RANGE_KEY + assembleKey + ":*");
        redissonClient.getKeys().deleteByPattern(Constants.RedisKeys.STRATEGY_RATE_TABLE_KEY + assembleKey + ":*");
        redissonClient.getKeys().deleteByPattern(
                Constants.RedisKeys.STRATEGY_PROBABILITY_RANGE_TABLE_KEY + assembleKey + ":*");
        redissonClient.getKeys().deleteByPattern(Constants.RedisKeys.STRATEGY_ALIAS_TABLE_KEY + assembleKey + ":*");
    }
}
