package com.lavyoung.marketforge;

import com.lavyoung.marketforge.domain.strategy.service.armorcy.StrategyArmoryDispatch;
import com.lavyoung.marketforge.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 使用开发环境 MySQL 与 Redis 验证策略装配及随机抽奖。
 *
 * @author lavyoung
 * @version 1.0.0-SNAPSHOT
 */
@SpringBootTest
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "RUN_DEV_INTEGRATION_TESTS", matches = "true")
@Slf4j
class StrategyArmoryDispatchDevIntegrationTest {

    private static final long STRATEGY_ID = 900_831_001L;
    private static final int DRAW_COUNT = 1_000;
    private static final Set<Long> EXPECTED_AWARD_IDS = Set.of(900_831_011L, 900_831_012L, 900_831_013L);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StrategyArmoryDispatch strategyArmoryDispatch;

    /**
     * 写入独立的开发验证数据，装配概率表并重复抽取验证结果范围。
     */
    @Test
    void shouldAssembleAndDrawAwardsFromDevelopmentData() {
        prepareDevelopmentAwards();
        clearAssembledStrategyCache();

        strategyArmoryDispatch.assembleLotteryStrategy(STRATEGY_ID);

        assertRandomAwardResults();
    }

    /**
     * 预置与开发数据对应的概率查找表，独立验证随机奖品接口。
     */
    @Test
    void shouldGetRandomAwardIdFromAssembledDevelopmentCache() {
        prepareDevelopmentAwards();
        clearAssembledStrategyCache();
        seedAssembledStrategyCache();

        assertRandomAwardResults();
    }

    private void prepareDevelopmentAwards() {
        insertOrUpdateAward(900_831_011L, "开发验证一等奖", new BigDecimal("0.10000"), 1);
        insertOrUpdateAward(900_831_012L, "开发验证二等奖", new BigDecimal("0.30000"), 2);
        insertOrUpdateAward(900_831_013L, "开发验证参与奖", new BigDecimal("0.60000"), 3);
    }

    private void assertRandomAwardResults() {
        Map<Long, Integer> awardCounts = new HashMap<>();
        for (int i = 0; i < DRAW_COUNT; i++) {
            long awardId = strategyArmoryDispatch.getRandomAwardId(STRATEGY_ID);
            assertTrue(EXPECTED_AWARD_IDS.contains(awardId), "返回了未配置的奖品: " + awardId);
            awardCounts.merge(awardId, 1, Integer::sum);
        }

        assertEquals(EXPECTED_AWARD_IDS, awardCounts.keySet());
        assertEquals(DRAW_COUNT, awardCounts.values().stream().mapToInt(Integer::intValue).sum());
        log.info("开发环境随机抽奖验证完成，strategyId={}，抽取次数={}，结果={}", STRATEGY_ID, DRAW_COUNT, awardCounts);
    }

    private void insertOrUpdateAward(long awardId, String title, BigDecimal rate, int sort) {
        String sql = """
                INSERT INTO strategy_award
                    (strategy_id, award_id, award_title, award_subtitle, award_count,
                     award_count_surplus, award_rate, rule_models, sort)
                SELECT ?, ?, ?, ?, ?, ?, ?, ?, ?
                WHERE NOT EXISTS (
                    SELECT 1 FROM strategy_award WHERE strategy_id = ? AND award_id = ?
                )
                """;
        jdbcTemplate.update(
                sql,
                STRATEGY_ID,
                awardId,
                title,
                "Codex 开发环境接口验证数据",
                10_000,
                10_000,
                rate,
                null,
                sort,
                STRATEGY_ID,
                awardId
        );
        jdbcTemplate.update(
                """
                        UPDATE strategy_award
                        SET award_title = ?, award_subtitle = ?, award_count = ?,
                            award_count_surplus = ?, award_rate = ?, rule_models = ?, sort = ?
                        WHERE strategy_id = ? AND award_id = ?
                        """,
                title,
                "Codex 开发环境接口验证数据",
                10_000,
                10_000,
                rate,
                null,
                sort,
                STRATEGY_ID,
                awardId
        );
    }

    private void clearAssembledStrategyCache() {
        redissonClient.getKeys().delete(
                Constants.RedisKeys.STRATEGY_AWARD_KEY + STRATEGY_ID,
                Constants.RedisKeys.STRATEGY_RATE_RANGE_KEY + STRATEGY_ID,
                Constants.RedisKeys.STRATEGY_RATE_TABLE_KEY + STRATEGY_ID
        );
    }

    private void seedAssembledStrategyCache() {
        String rangeKey = Constants.RedisKeys.STRATEGY_RATE_RANGE_KEY + STRATEGY_ID;
        String tableKey = Constants.RedisKeys.STRATEGY_RATE_TABLE_KEY + STRATEGY_ID;
        Map<Integer, Long> lookupTable = Map.of(
                0, 900_831_011L,
                1, 900_831_012L,
                2, 900_831_012L,
                3, 900_831_012L,
                4, 900_831_013L,
                5, 900_831_013L,
                6, 900_831_013L,
                7, 900_831_013L,
                8, 900_831_013L,
                9, 900_831_013L
        );
        redissonClient.getBucket(rangeKey).set(lookupTable.size());
        redissonClient.getMap(tableKey).putAll(lookupTable);
    }
}
