package com.lavyoung.marketforge;

import com.lavyoung.marketforge.app.MarketForgeApplication;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyArmory;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyDispatch;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilityAlgorithmType;
import com.lavyoung.marketforge.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 使用开发环境 MySQL 与 Redis 验证策略装配及随机抽奖。
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
    private static final Set<Long> ASSEMBLED_AWARD_IDS = Set.of(
            900_901_011L, 900_901_012L, 900_901_013L, 900_901_014L, 900_901_015L,
            900_901_016L, 900_901_017L, 900_901_018L, 900_901_019L
    );
    private static final Set<Long> SEEDED_CACHE_AWARD_IDS = Set.of(900_901_011L, 900_901_012L, 900_901_013L);

    private final RedissonClient redissonClient;
    private final IStrategyArmory strategyArmory;
    private final IStrategyDispatch strategyDispatch;

    /**
     * 创建使用开发环境真实基础设施的策略装配集成测试。
     *
     * @param redissonClient   开发环境 Redis 客户端
     * @param strategyArmory   策略装配领域服务
     * @param strategyDispatch 策略派发领域服务
     */
    @Autowired
    public StrategyArmoryDispatchDevIntegrationTest(RedissonClient redissonClient, IStrategyArmory strategyArmory,
                                                    IStrategyDispatch strategyDispatch) {
        this.redissonClient = redissonClient;
        this.strategyArmory = strategyArmory;
        this.strategyDispatch = strategyDispatch;
    }

    /**
     * 使用正式初始化数据装配概率空间，并重复抽取验证结果范围。
     */
    @Test
    void shouldAssembleAndDrawAwardsFromDevelopmentData() {
        clearAssembledStrategyCache();

        strategyArmory.assembleLotteryStrategy(STRATEGY_ID);

        assertRandomAwardResults(ASSEMBLED_AWARD_IDS);
    }

    /**
     * 预置与开发数据对应的概率查找表，独立验证随机奖品接口。
     */
    @Test
    void shouldGetRandomAwardIdFromAssembledDevelopmentCache() {
        clearAssembledStrategyCache();
        seedAssembledStrategyCache();

        assertRandomAwardResults(SEEDED_CACHE_AWARD_IDS);
    }

    /**
     * 清理测试写入的策略装配缓存，避免两个用例相互污染开发环境。
     */
    @AfterEach
    void cleanUpAssembledStrategyCache() {
        clearAssembledStrategyCache();
    }

    private void assertRandomAwardResults(Set<Long> expectedAwardIds) {
        Map<Long, Integer> awardCounts = new HashMap<>();
        for (int i = 0; i < DRAW_COUNT; i++) {
            long awardId = strategyDispatch.getRandomAwardId(STRATEGY_ID);
            assertTrue(expectedAwardIds.contains(awardId), "返回了未配置的奖品: " + awardId);
            awardCounts.merge(awardId, 1, Integer::sum);
        }

        assertFalse(awardCounts.isEmpty());
        assertEquals(DRAW_COUNT, awardCounts.values().stream().mapToInt(Integer::intValue).sum());
        log.info("开发环境随机抽奖验证完成，strategyId={}，抽取次数={}，结果={}", STRATEGY_ID, DRAW_COUNT, awardCounts);
    }

    private void clearAssembledStrategyCache() {
        clearProbabilitySpaceCache(String.valueOf(STRATEGY_ID));
        clearProbabilitySpaceCache(STRATEGY_ID + Constants.UNDERLINE + "40000");
        clearProbabilitySpaceCache(STRATEGY_ID + Constants.UNDERLINE + "60000");
    }

    private void clearProbabilitySpaceCache(String assembleKey) {
        redissonClient.getKeys().delete(
                Constants.RedisKeys.STRATEGY_AWARD_KEY + assembleKey,
                Constants.RedisKeys.STRATEGY_RATE_RANGE_KEY + assembleKey,
                Constants.RedisKeys.STRATEGY_RATE_TABLE_KEY + assembleKey,
                Constants.RedisKeys.STRATEGY_PROBABILITY_RANGE_TABLE_KEY + assembleKey,
                Constants.RedisKeys.STRATEGY_PROBABILITY_ALGORITHM_KEY + assembleKey
        );
    }

    private void seedAssembledStrategyCache() {
        String rangeKey = Constants.RedisKeys.STRATEGY_RATE_RANGE_KEY + STRATEGY_ID;
        String tableKey = Constants.RedisKeys.STRATEGY_RATE_TABLE_KEY + STRATEGY_ID;
        Map<Integer, Long> lookupTable = Map.of(
                0, 900_901_011L,
                1, 900_901_012L,
                2, 900_901_012L,
                3, 900_901_012L,
                4, 900_901_013L,
                5, 900_901_013L,
                6, 900_901_013L,
                7, 900_901_013L,
                8, 900_901_013L,
                9, 900_901_013L
        );
        redissonClient.getBucket(rangeKey).set(lookupTable.size());
        redissonClient.getMap(tableKey).putAll(lookupTable);
        redissonClient.getBucket(Constants.RedisKeys.STRATEGY_PROBABILITY_ALGORITHM_KEY + STRATEGY_ID)
                .set(ProbabilityAlgorithmType.DIRECT_LOOKUP.getCode());
    }
}
