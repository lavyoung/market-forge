package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.lavyoung.marketforge.domain.strategy.event.AwardStockDeductedEvent;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardStockKeyVO;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilityAlgorithmType;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AliasProbabilityTable;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityRange;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.ProbabilitySnapshotKey;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.ProbabilitySnapshotPointer;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.AwardAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.StrategyAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.StrategyAwardAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.StrategyRuleAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.*;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyAwardPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyRulePO;
import com.lavyoung.marketforge.infrastructure.persistent.redis.IRedisService;
import com.lavyoung.marketforge.infrastructure.persistent.repository.cache.ProbabilitySnapshotLocalCache;
import com.lavyoung.marketforge.infrastructure.persistent.repository.config.ProbabilitySnapshotLocalCacheProperties;
import com.lavyoung.marketforge.infrastructure.persistent.repository.config.ProbabilitySnapshotRetentionProperties;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.messaging.MessagePublisher;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 验证 {@link StrategyRepository} 对策略级规则值和奖品规则模型的查询转换行为。
 */
@ExtendWith(MockitoExtension.class)
class StrategyRepositoryTest {

    private static final Long STRATEGY_ID = 100_001L;
    private static final long AWARD_ID = 100_011L;
    private static final String STOCK_KEY = "strategy_award_stock:100001_100011";
    private static final String STOCK_LOCK_KEY = STOCK_KEY + ":lock";
    private static final String PROBABILITY_KEY = "100001";
    private static final String SNAPSHOT_POINTER_KEY =
            Constants.RedisKeys.STRATEGY_PROBABILITY_SNAPSHOT_POINTER_KEY + PROBABILITY_KEY;
    private static final String SNAPSHOT_PUBLISH_LOCK_KEY =
            Constants.RedisKeys.STRATEGY_PROBABILITY_SNAPSHOT_PUBLISH_LOCK_KEY + PROBABILITY_KEY;
    private static final int TOTAL_WEIGHT = 100;
    private static final Duration DEFAULT_STOCK_LOCK_LEASE_TIME = Duration.ofSeconds(30);
    private static final Duration SNAPSHOT_RETIREMENT_GRACE_PERIOD = Duration.ofMinutes(10);
    private static final Duration LOCAL_CACHE_EXPIRE_AFTER_ACCESS = Duration.ofMinutes(30);

    @Mock
    private IStrategyAwardDao strategyAwardDao;

    @Mock
    private IStrategyDao strategyDao;

    @Mock
    private IStrategyRuleDao strategyRuleDao;

    @Mock
    private IUserAwardRecordDao userAwardRecordDao;

    @Mock
    private IAwardDao awardDao;

    @Mock
    private IRedisService redisService;

    @Mock
    private StrategyAwardAssembler strategyAwardAssembler;

    @Mock
    private StrategyAssembler strategyAssembler;

    @Mock
    private StrategyRuleAssembler strategyRuleAssembler;

    @Mock
    private AwardAssembler awardAssembler;

    @Mock
    private MessagePublisher messagePublisher;

    private StrategyRepository repository;
    private StrategyAwardStockMessageRepository stockMessageRepository;

    /**
     * Given 模拟基础设施依赖，When 初始化仓储实现，Then 使用可控 DAO 返回值执行测试。
     */
    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "StrategyRepositoryTest"),
                StrategyRulePO.class
        );
        ProbabilitySnapshotLocalCache localCache = new ProbabilitySnapshotLocalCache(
                new ProbabilitySnapshotLocalCacheProperties(
                        true, LOCAL_CACHE_EXPIRE_AFTER_ACCESS, 1_000L, 10_000L));
        repository = new StrategyRepository(
                strategyAwardDao,
                strategyDao,
                strategyRuleDao,
                userAwardRecordDao,
                awardDao,
                redisService,
                localCache,
                new ProbabilitySnapshotRetentionProperties(SNAPSHOT_RETIREMENT_GRACE_PERIOD),
                strategyAwardAssembler,
                strategyAssembler,
                strategyRuleAssembler,
                awardAssembler
        );
        stockMessageRepository = new StrategyAwardStockMessageRepository(redisService, messagePublisher);
    }

    /**
     * Given 数据库存在策略级权重规则，When 使用便捷重载查询，Then 返回规则配置值。
     */
    @Test
    void shouldQueryStrategyLevelRuleValue() {
        // Given
        StrategyRulePO rule = new StrategyRulePO();
        rule.setRuleValue("4000:100011/100012");
        when(strategyRuleDao.selectOne(any(Wrapper.class))).thenReturn(rule);

        // When
        String result = repository.queryStrategyRuleValue(STRATEGY_ID, RuleModel.WEIGHT.getCode());

        // Then
        assertEquals(rule.getRuleValue(), result);
        verify(strategyRuleDao).selectOne(any(Wrapper.class));
    }

    /**
     * Given 数据库存在策略奖品配置，When 查询奖品规则模型，Then 转换为领域值对象。
     */
    @Test
    void shouldMapStrategyAwardRuleModels() {
        // Given
        StrategyAwardPO award = new StrategyAwardPO();
        award.setRuleModels("rule_lock/rule_luck_award");
        when(strategyAwardDao.selectOne(any(Wrapper.class))).thenReturn(award);

        // When
        StrategyAwardRuleModelVO result = repository.queryStrategyAwardRuleModels(STRATEGY_ID, AWARD_ID);

        // Then
        assertEquals(award.getRuleModels(), result.ruleModels());
        verify(strategyAwardDao).selectOne(any(Wrapper.class));
    }

    /**
     * Given 数据库不存在策略奖品配置，When 查询奖品规则模型，Then 返回空值。
     */
    @Test
    void shouldReturnNullWhenStrategyAwardDoesNotExist() {
        // Given
        when(strategyAwardDao.selectOne(any(Wrapper.class))).thenReturn(null);

        // When
        StrategyAwardRuleModelVO result = repository.queryStrategyAwardRuleModels(STRATEGY_ID, AWARD_ID);

        // Then
        assertNull(result);
    }

    /**
     * Given 多个奖品配置次数锁且请求包含重复奖品标识，When 批量查询解锁门槛，Then 去重后只查询一次并按奖品标识返回结果。
     */
    @Test
    void shouldQueryAwardRuleLockCountsInSingleBatch() {
        // Given
        StrategyRulePO twoTimesRule = newLockRule(AWARD_ID, "2");
        StrategyRulePO fiveTimesRule = newLockRule(100_012L, "5");
        when(strategyRuleDao.selectList(any(Wrapper.class)))
                .thenReturn(List.of(twoTimesRule, fiveTimesRule));

        // When
        Map<Long, Integer> result = repository.queryAwardRuleLockCounts(
                STRATEGY_ID,
                List.of(AWARD_ID, 100_012L, AWARD_ID)
        );

        // Then
        assertEquals(Map.of(AWARD_ID, 2, 100_012L, 5), result);
        verify(strategyRuleDao).selectList(any(Wrapper.class));
    }

    /**
     * Given 页面奖品列表不包含次数锁奖品，When 查询解锁门槛，Then 返回空映射且不访问数据库。
     */
    @Test
    void shouldSkipRuleQueryWhenAwardIdsAreEmpty() {
        // When
        Map<Long, Integer> result = repository.queryAwardRuleLockCounts(STRATEGY_ID, List.of());

        // Then
        assertTrue(result.isEmpty());
        verifyNoInteractions(strategyRuleDao);
    }

    /**
     * Given 奖品声明了次数锁但数据库缺少对应规则，When 批量查询解锁门槛，Then 抛出规则不存在业务异常。
     */
    @Test
    void shouldRejectMissingAwardLockRule() {
        // Given
        when(strategyRuleDao.selectList(any(Wrapper.class)))
                .thenReturn(List.of(newLockRule(AWARD_ID, "2")));

        // When
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> repository.queryAwardRuleLockCounts(
                        STRATEGY_ID,
                        List.of(AWARD_ID, 100_012L)
                )
        );

        // Then
        assertEquals(BusinessResponseCode.STRATEGY_RULE_NOT_FOUND, exception.getResponseCode());
        verify(strategyRuleDao).selectList(any(Wrapper.class));
    }

    /**
     * Given 次数锁规则值不是非负整数，When 批量查询解锁门槛，Then 抛出规则值非法业务异常。
     */
    @Test
    void shouldRejectInvalidAwardLockRuleValue() {
        // Given
        when(strategyRuleDao.selectList(any(Wrapper.class)))
                .thenReturn(List.of(newLockRule(AWARD_ID, "strategy_rule")));

        // When
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> repository.queryAwardRuleLockCounts(STRATEGY_ID, List.of(AWARD_ID))
        );

        // Then
        assertEquals(BusinessResponseCode.STRATEGY_RULE_VALUE_INVALID, exception.getResponseCode());
        verify(strategyRuleDao).selectList(any(Wrapper.class));
    }

    /**
     * Given 次数锁规则值前后包含空格，When 批量查询解锁门槛，Then 应由领域值对象标准化并返回整数。
     */
    @Test
    void shouldNormalizeAwardLockRuleValue() {
        // Given
        when(strategyRuleDao.selectList(any(Wrapper.class))).thenReturn(List.of(newLockRule(AWARD_ID, " 2 ")));

        // When
        Map<Long, Integer> result = repository.queryAwardRuleLockCounts(STRATEGY_ID, List.of(AWARD_ID));

        // Then
        assertEquals(Map.of(AWARD_ID, 2), result);
        verify(strategyRuleDao).selectList(any(Wrapper.class));
    }

    /**
     * Given 次数锁规则值为负数，When 批量查询解锁门槛，Then 应转换为统一的规则值非法业务异常。
     */
    @Test
    void shouldRejectNegativeAwardLockRuleValue() {
        // Given
        when(strategyRuleDao.selectList(any(Wrapper.class))).thenReturn(List.of(newLockRule(AWARD_ID, "-1")));

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> repository.queryAwardRuleLockCounts(STRATEGY_ID, List.of(AWARD_ID)));

        // Then
        assertEquals(BusinessResponseCode.STRATEGY_RULE_VALUE_INVALID, exception.getResponseCode());
        verify(strategyRuleDao).selectList(any(Wrapper.class));
    }

    /**
     * Given Redis 库存充足，When 扣减奖品库存，Then 在分布式锁保护下完成原子扣减并释放锁。
     */
    @Test
    void shouldSubtractAwardStockWithinDistributedLock() {
        // Given
        when(redisService.getAtomicLong(STOCK_KEY)).thenReturn(10L);

        // When
        boolean subtracted = repository.subtractAwardStock(STRATEGY_ID, AWARD_ID, 2);

        // Then
        assertTrue(subtracted);
        verify(redisService).lock(STOCK_LOCK_KEY, DEFAULT_STOCK_LOCK_LEASE_TIME);
        verify(redisService).addAndGetAtomicLong(STOCK_KEY, -2L);
        verify(redisService).unlock(STOCK_LOCK_KEY);
    }

    /**
     * Given Redis 库存不足，When 尝试扣减奖品库存，Then 不修改计数器但仍释放分布式锁。
     */
    @Test
    void shouldRejectInsufficientAwardStockAndReleaseLock() {
        // Given
        when(redisService.getAtomicLong(STOCK_KEY)).thenReturn(1L);

        // When
        boolean subtracted = repository.subtractAwardStock(STRATEGY_ID, AWARD_ID, 2);

        // Then
        assertFalse(subtracted);
        verify(redisService).lock(STOCK_LOCK_KEY, DEFAULT_STOCK_LOCK_LEASE_TIME);
        verify(redisService, never()).addAndGetAtomicLong(STOCK_KEY, -2L);
        verify(redisService).unlock(STOCK_LOCK_KEY);
    }

    /**
     * Given 非法库存参数，When 尝试扣减库存，Then 在访问 Redis 前拒绝请求。
     */
    @Test
    void shouldRejectInvalidAwardStockArguments() {
        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> repository.subtractAwardStock(null, AWARD_ID, 1)),
                () -> assertThrows(NullPointerException.class,
                        () -> repository.subtractAwardStock(STRATEGY_ID, null, 1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> repository.subtractAwardStock(STRATEGY_ID, AWARD_ID, 0))
        );
        verify(redisService, never()).lock(STOCK_LOCK_KEY);
    }

    /**
     * Given 已完成装配的累计概率区间，When 持久化二分算法数据，Then 分别写入总权重和不可变区间快照。
     */
    @Test
    void shouldStoreStrategyAwardProbabilityRanges() {
        // Given
        List<AwardProbabilityRange> probabilityRanges = List.of(
                new AwardProbabilityRange(60, 100_011L),
                new AwardProbabilityRange(90, 100_012L),
                new AwardProbabilityRange(100, 100_013L)
        );

        // When
        repository.storeStrategyAwardProbabilityRanges(PROBABILITY_KEY, TOTAL_WEIGHT, probabilityRanges);

        // Then
        verify(redisService).setValue(Constants.RedisKeys.STRATEGY_RATE_RANGE_KEY + PROBABILITY_KEY, TOTAL_WEIGHT);
        verify(redisService).setValue(Constants.RedisKeys.STRATEGY_PROBABILITY_RANGE_TABLE_KEY + PROBABILITY_KEY, probabilityRanges);
    }

    /**
     * Given Redis 存在累计概率区间，When 查询二分算法数据，Then 返回类型安全且顺序不变的区间列表。
     */
    @Test
    void shouldGetStrategyAwardProbabilityRanges() {
        // Given
        List<AwardProbabilityRange> probabilityRanges = List.of(
                new AwardProbabilityRange(60, 100_011L),
                new AwardProbabilityRange(100, 100_012L)
        );
        String cacheKey = Constants.RedisKeys.STRATEGY_PROBABILITY_RANGE_TABLE_KEY + PROBABILITY_KEY;
        when(redisService.getValueList(cacheKey, AwardProbabilityRange.class)).thenReturn(Optional.of(probabilityRanges));

        // When
        List<AwardProbabilityRange> result = repository.getStrategyAwardProbabilityRanges(PROBABILITY_KEY);

        // Then
        assertEquals(probabilityRanges, result);
        verify(redisService).getValueList(cacheKey, AwardProbabilityRange.class);
    }

    /**
     * Given Redis 尚未装配累计概率区间，When 查询二分算法数据，Then 返回空列表而不是空引用。
     */
    @Test
    void shouldReturnEmptyProbabilityRangesWhenCacheIsMissing() {
        // Given
        String cacheKey = Constants.RedisKeys.STRATEGY_PROBABILITY_RANGE_TABLE_KEY + PROBABILITY_KEY;
        when(redisService.getValueList(cacheKey, AwardProbabilityRange.class)).thenReturn(Optional.empty());

        // When
        List<AwardProbabilityRange> result = repository.getStrategyAwardProbabilityRanges(PROBABILITY_KEY);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * Given 已完成装配的 Alias 概率表，When 保存概率快照，Then 使用独立 Redis Key 写入完整对象。
     */
    @Test
    void shouldStoreStrategyAwardAliasTable() {
        // Given
        AliasProbabilityTable aliasTable = aliasProbabilityTable();
        String cacheKey = Constants.RedisKeys.STRATEGY_ALIAS_TABLE_KEY + PROBABILITY_KEY;

        // When
        repository.storeStrategyAwardAliasTable(PROBABILITY_KEY, aliasTable);

        // Then
        verify(redisService).setValue(cacheKey, aliasTable);
    }

    /**
     * Given Redis 中存在 Alias 概率表，When 按装配键读取，Then 返回完整概率快照。
     */
    @Test
    void shouldGetStrategyAwardAliasTable() {
        // Given
        AliasProbabilityTable aliasTable = aliasProbabilityTable();
        String cacheKey = Constants.RedisKeys.STRATEGY_ALIAS_TABLE_KEY + PROBABILITY_KEY;
        when(redisService.getValue(cacheKey, AliasProbabilityTable.class)).thenReturn(Optional.of(aliasTable));

        // When
        Optional<AliasProbabilityTable> result = repository.getStrategyAwardAliasTable(PROBABILITY_KEY);

        // Then
        assertEquals(Optional.of(aliasTable), result);
        verify(redisService).getValue(cacheKey, AliasProbabilityTable.class);
    }

    /**
     * Given Redis 中没有 Alias 概率表，When 按装配键读取，Then 返回空结果表示尚未装配。
     */
    @Test
    void shouldReturnEmptyWhenAliasTableDoesNotExist() {
        // Given
        String cacheKey = Constants.RedisKeys.STRATEGY_ALIAS_TABLE_KEY + PROBABILITY_KEY;
        when(redisService.getValue(cacheKey, AliasProbabilityTable.class)).thenReturn(Optional.empty());

        // When
        Optional<AliasProbabilityTable> result = repository.getStrategyAwardAliasTable(PROBABILITY_KEY);

        // Then
        assertTrue(result.isEmpty());
        verify(redisService).getValue(cacheKey, AliasProbabilityTable.class);
    }

    /**
     * Given Alias 概率表为空，When 尝试保存，Then 在访问 Redis 前立即拒绝无效数据。
     */
    @Test
    void shouldRejectNullAliasTable() {
        // Given
        AliasProbabilityTable aliasTable = null;

        // When
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> repository.storeStrategyAwardAliasTable(PROBABILITY_KEY, aliasTable));

        // Then
        assertEquals("aliasTable must not be null", exception.getMessage());
        verifyNoInteractions(redisService);
    }

    /**
     * Given 策略选择了二分算法，When 保存装配元数据，Then Redis 仅保存稳定算法编码。
     */
    @Test
    void shouldStoreStrategyProbabilityAlgorithmCode() {
        repository.storeStrategyProbabilityAlgorithm(PROBABILITY_KEY, ProbabilityAlgorithmType.BINARY_SEARCH);

        verify(redisService).setValue(Constants.RedisKeys.STRATEGY_PROBABILITY_ALGORITHM_KEY + PROBABILITY_KEY, "binary_search");
    }

    /**
     * Given Redis 存在已知算法编码，When 查询装配元数据，Then 转换为对应算法类型。
     */
    @Test
    void shouldGetStrategyProbabilityAlgorithm() {
        String cacheKey = Constants.RedisKeys.STRATEGY_PROBABILITY_ALGORITHM_KEY + PROBABILITY_KEY;
        when(redisService.getValue(cacheKey, String.class)).thenReturn(Optional.of("direct_lookup"));

        Optional<ProbabilityAlgorithmType> result = repository.getStrategyProbabilityAlgorithm(PROBABILITY_KEY);

        assertEquals(Optional.of(ProbabilityAlgorithmType.DIRECT_LOOKUP), result);
    }

    /**
     * Given Redis 中存在无法识别的算法编码，When 查询装配元数据，Then 返回空结果避免误用其他算法。
     */
    @Test
    void shouldReturnEmptyWhenProbabilityAlgorithmCodeIsUnknown() {
        String cacheKey = Constants.RedisKeys.STRATEGY_PROBABILITY_ALGORITHM_KEY + PROBABILITY_KEY;
        when(redisService.getValue(cacheKey, String.class)).thenReturn(Optional.of("unknown_algorithm"));

        Optional<ProbabilityAlgorithmType> result = repository.getStrategyProbabilityAlgorithm(PROBABILITY_KEY);

        assertTrue(result.isEmpty());
    }

    /**
     * Given 同一概率空间的新快照版本，When 保存并读取直接查表数据，Then 所有 Redis Key 使用相同版本后缀。
     */
    @Test
    void shouldStoreAndReadVersionedDirectLookupSnapshot() {
        // Given
        ProbabilitySnapshotKey snapshotKey = new ProbabilitySnapshotKey(PROBABILITY_KEY, 7L);
        Map<Integer, Long> lookupTable = Map.of(0, 100_011L, 1, 100_012L);
        String rateRangeKey = Constants.RedisKeys.STRATEGY_RATE_RANGE_KEY + PROBABILITY_KEY + ":7";
        String rateTableKey = Constants.RedisKeys.STRATEGY_RATE_TABLE_KEY + PROBABILITY_KEY + ":7";
        when(redisService.getValue(rateRangeKey, Integer.class)).thenReturn(Optional.of(2));
        when(redisService.getHashEntries(rateTableKey, Integer.class, Long.class)).thenReturn(lookupTable);

        // When
        repository.storeStrategyAwardSearchTables(snapshotKey, 2, lookupTable);
        int rateRange = repository.getRateRange(snapshotKey);
        long awardId = repository.getStrategyAwardAssemble(snapshotKey, 1);
        int cachedRateRange = repository.getRateRange(snapshotKey);
        long cachedAwardId = repository.getStrategyAwardAssemble(snapshotKey, 1);

        // Then
        assertEquals(2, rateRange);
        assertEquals(100_012L, awardId);
        assertEquals(rateRange, cachedRateRange);
        assertEquals(awardId, cachedAwardId);
        verify(redisService).setValue(rateRangeKey, 2);
        verify(redisService).putHashValues(rateTableKey, lookupTable);
        verify(redisService, times(1)).getValue(rateRangeKey, Integer.class);
        verify(redisService, times(1)).getHashEntries(rateTableKey, Integer.class, Long.class);
    }

    /**
     * Given 同一概率空间的新快照版本，When 保存并读取二分区间数据，Then 总权重和区间使用同一版本后缀。
     */
    @Test
    void shouldStoreAndReadVersionedBinarySearchSnapshot() {
        // Given
        ProbabilitySnapshotKey snapshotKey = new ProbabilitySnapshotKey(PROBABILITY_KEY, 8L);
        List<AwardProbabilityRange> ranges = List.of(
                new AwardProbabilityRange(60, 100_011L),
                new AwardProbabilityRange(100, 100_012L)
        );
        String rateRangeKey = Constants.RedisKeys.STRATEGY_RATE_RANGE_KEY + PROBABILITY_KEY + ":8";
        String rangeTableKey = Constants.RedisKeys.STRATEGY_PROBABILITY_RANGE_TABLE_KEY + PROBABILITY_KEY + ":8";
        when(redisService.getValueList(rangeTableKey, AwardProbabilityRange.class)).thenReturn(Optional.of(ranges));

        // When
        repository.storeStrategyAwardProbabilityRanges(snapshotKey, TOTAL_WEIGHT, ranges);
        List<AwardProbabilityRange> result = repository.getStrategyAwardProbabilityRanges(snapshotKey);
        List<AwardProbabilityRange> cachedResult = repository.getStrategyAwardProbabilityRanges(snapshotKey);

        // Then
        assertEquals(ranges, result);
        assertEquals(result, cachedResult);
        verify(redisService).setValue(rateRangeKey, TOTAL_WEIGHT);
        verify(redisService).setValue(rangeTableKey, ranges);
        verify(redisService, times(1)).getValueList(rangeTableKey, AwardProbabilityRange.class);
    }

    /**
     * Given 同一概率空间的新快照版本，When 保存并读取 Alias 数据，Then 完整概率表写入版本专属 Key。
     */
    @Test
    void shouldStoreAndReadVersionedAliasSnapshot() {
        // Given
        ProbabilitySnapshotKey snapshotKey = new ProbabilitySnapshotKey(PROBABILITY_KEY, 9L);
        AliasProbabilityTable aliasTable = aliasProbabilityTable();
        String aliasTableKey = Constants.RedisKeys.STRATEGY_ALIAS_TABLE_KEY + PROBABILITY_KEY + ":9";
        when(redisService.getValue(aliasTableKey, AliasProbabilityTable.class)).thenReturn(Optional.of(aliasTable));

        // When
        repository.storeStrategyAwardAliasTable(snapshotKey, aliasTable);
        Optional<AliasProbabilityTable> result = repository.getStrategyAwardAliasTable(snapshotKey);
        Optional<AliasProbabilityTable> cachedResult = repository.getStrategyAwardAliasTable(snapshotKey);

        // Then
        assertEquals(Optional.of(aliasTable), result);
        assertEquals(result, cachedResult);
        verify(redisService).setValue(aliasTableKey, aliasTable);
        verify(redisService, times(1)).getValue(aliasTableKey, AliasProbabilityTable.class);
    }

    /**
     * Given 快照键为空，When 调用任一版本化仓储入口，Then 在访问 Redis 前拒绝无身份的数据。
     */
    @Test
    void shouldRejectNullSnapshotKeyForVersionedStorage() {
        AliasProbabilityTable aliasTable = aliasProbabilityTable();

        assertAll(
                () -> assertThrows(NullPointerException.class,
                        () -> repository.storeStrategyAwardSearchTables((ProbabilitySnapshotKey) null, 1, Map.of(0, 100_011L))),
                () -> assertThrows(NullPointerException.class, () -> repository.getRateRange((ProbabilitySnapshotKey) null)),
                () -> assertThrows(NullPointerException.class,
                        () -> repository.getStrategyAwardAssemble((ProbabilitySnapshotKey) null, 0)),
                () -> assertThrows(NullPointerException.class,
                        () -> repository.storeStrategyAwardProbabilityRanges((ProbabilitySnapshotKey) null, 1, List.of())),
                () -> assertThrows(NullPointerException.class,
                        () -> repository.getStrategyAwardProbabilityRanges((ProbabilitySnapshotKey) null)),
                () -> assertThrows(NullPointerException.class,
                        () -> repository.storeStrategyAwardAliasTable((ProbabilitySnapshotKey) null, aliasTable)),
                () -> assertThrows(NullPointerException.class,
                        () -> repository.getStrategyAwardAliasTable((ProbabilitySnapshotKey) null))
        );
        verifyNoInteractions(redisService);
    }

    /**
     * Given Redis 为装配键生成了新版本，When 申请快照版本，Then 返回原子递增结果。
     */
    @Test
    void shouldGenerateNextProbabilitySnapshotVersion() {
        // Given
        String versionKey = Constants.RedisKeys.STRATEGY_PROBABILITY_SNAPSHOT_VERSION_KEY + PROBABILITY_KEY;
        when(redisService.incrementAndGetAtomicLong(versionKey)).thenReturn(3L);

        // When
        long version = repository.nextProbabilitySnapshotVersion(PROBABILITY_KEY);

        // Then
        assertEquals(3L, version);
        verify(redisService).incrementAndGetAtomicLong(versionKey);
    }

    /**
     * Given 当前没有生效指针，When 发布第一版快照，Then 在锁内保存并释放发布锁。
     */
    @Test
    void shouldPublishFirstProbabilitySnapshotPointer() {
        // Given
        ProbabilitySnapshotPointer pointer = snapshotPointer(1L);
        when(redisService.getValue(SNAPSHOT_POINTER_KEY, ProbabilitySnapshotPointer.class)).thenReturn(Optional.empty());

        // When
        boolean published = repository.publishProbabilitySnapshotPointer(pointer);

        // Then
        assertTrue(published);
        var ordered = inOrder(redisService);
        ordered.verify(redisService).lock(SNAPSHOT_PUBLISH_LOCK_KEY);
        ordered.verify(redisService).getValue(SNAPSHOT_POINTER_KEY, ProbabilitySnapshotPointer.class);
        ordered.verify(redisService).setValue(SNAPSHOT_POINTER_KEY, pointer);
        ordered.verify(redisService).unlock(SNAPSHOT_PUBLISH_LOCK_KEY);
        verify(redisService, never()).expire(anyString(), any(Duration.class));
    }

    /**
     * Given 当前生效版本低于候选版本，When 发布新快照，Then 使用新指针替换旧指针。
     */
    @Test
    void shouldPublishNewerProbabilitySnapshotPointer() {
        // Given
        ProbabilitySnapshotPointer activePointer = snapshotPointer(2L);
        ProbabilitySnapshotPointer candidatePointer = snapshotPointer(3L);
        when(redisService.getValue(SNAPSHOT_POINTER_KEY, ProbabilitySnapshotPointer.class))
                .thenReturn(Optional.of(activePointer));

        // When
        boolean published = repository.publishProbabilitySnapshotPointer(candidatePointer);

        // Then
        assertTrue(published);
        String retiredSnapshotKey = Constants.RedisKeys.STRATEGY_ALIAS_TABLE_KEY + PROBABILITY_KEY + ":2";
        var ordered = inOrder(redisService);
        ordered.verify(redisService).setValue(SNAPSHOT_POINTER_KEY, candidatePointer);
        ordered.verify(redisService).expire(retiredSnapshotKey, SNAPSHOT_RETIREMENT_GRACE_PERIOD);
        ordered.verify(redisService).unlock(SNAPSHOT_PUBLISH_LOCK_KEY);
    }

    /**
     * Given 当前版本不低于候选版本，When 旧装配任务尝试发布，Then 拒绝回滚且仍释放锁。
     */
    @Test
    void shouldRejectStaleProbabilitySnapshotPointer() {
        // Given
        ProbabilitySnapshotPointer activePointer = snapshotPointer(3L);
        ProbabilitySnapshotPointer stalePointer = snapshotPointer(2L);
        when(redisService.getValue(SNAPSHOT_POINTER_KEY, ProbabilitySnapshotPointer.class))
                .thenReturn(Optional.of(activePointer));

        // When
        boolean published = repository.publishProbabilitySnapshotPointer(stalePointer);

        // Then
        assertFalse(published);
        verify(redisService, never()).setValue(SNAPSHOT_POINTER_KEY, stalePointer);
        verify(redisService, never()).expire(anyString(), any(Duration.class));
        verify(redisService).unlock(SNAPSHOT_PUBLISH_LOCK_KEY);
    }

    /**
     * Given 多个版本同时竞争同一个装配键，When 并发发布快照指针，Then 生效版本只能前进到最大版本。
     *
     * @throws Exception 并发任务等待被中断或执行失败时抛出
     */
    @Test
    void shouldKeepLatestProbabilitySnapshotPointerDuringConcurrentPublication() throws Exception {
        // Given
        int publisherCount = 20;
        AtomicReference<ProbabilitySnapshotPointer> activePointer = new AtomicReference<>();
        ReentrantLock publishLock = new ReentrantLock();
        CountDownLatch startGate = new CountDownLatch(1);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                4, 4, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        doAnswer(invocation -> {
            publishLock.lock();
            return null;
        }).when(redisService).lock(SNAPSHOT_PUBLISH_LOCK_KEY);
        doAnswer(invocation -> {
            publishLock.unlock();
            return null;
        }).when(redisService).unlock(SNAPSHOT_PUBLISH_LOCK_KEY);
        when(redisService.getValue(SNAPSHOT_POINTER_KEY, ProbabilitySnapshotPointer.class))
                .thenAnswer(invocation -> Optional.ofNullable(activePointer.get()));
        doAnswer(invocation -> {
            activePointer.set(invocation.getArgument(1));
            return null;
        }).when(redisService).setValue(eq(SNAPSHOT_POINTER_KEY), any(ProbabilitySnapshotPointer.class));
        when(redisService.expire(anyString(), eq(SNAPSHOT_RETIREMENT_GRACE_PERIOD))).thenReturn(true);

        try {
            List<CompletableFuture<Boolean>> publications = LongStream.rangeClosed(1L, publisherCount)
                    .mapToObj(version -> CompletableFuture.supplyAsync(() -> {
                        try {
                            startGate.await();
                            return repository.publishProbabilitySnapshotPointer(snapshotPointer(version));
                        } catch (InterruptedException exception) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("snapshot publication interrupted", exception);
                        }
                    }, executor))
                    .toList();

            // When
            startGate.countDown();
            CompletableFuture.allOf(publications.toArray(CompletableFuture[]::new)).get(10, TimeUnit.SECONDS);

            // Then
            assertEquals(publisherCount, activePointer.get().version());
            String activeSnapshotKey = Constants.RedisKeys.STRATEGY_ALIAS_TABLE_KEY
                    + PROBABILITY_KEY + ":" + publisherCount;
            verify(redisService, never()).expire(activeSnapshotKey, SNAPSHOT_RETIREMENT_GRACE_PERIOD);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    /**
     * Given 一份直接查表快照，When 将快照标记为退役，Then 范围值和完整查找表使用相同宽限期过期。
     */
    @Test
    void shouldRetireDirectLookupProbabilitySnapshot() {
        // Given
        ProbabilitySnapshotPointer pointer = new ProbabilitySnapshotPointer(
                PROBABILITY_KEY, ProbabilityAlgorithmType.DIRECT_LOOKUP, 7L);
        String suffix = PROBABILITY_KEY + ":7";

        // When
        repository.retireProbabilitySnapshot(pointer);

        // Then
        verify(redisService).expire(Constants.RedisKeys.STRATEGY_RATE_RANGE_KEY + suffix,
                SNAPSHOT_RETIREMENT_GRACE_PERIOD);
        verify(redisService).expire(Constants.RedisKeys.STRATEGY_RATE_TABLE_KEY + suffix,
                SNAPSHOT_RETIREMENT_GRACE_PERIOD);
    }

    /**
     * Given 直接查表快照已经进入本地缓存，When 退役该快照后再次读取，Then 清除本地数据并重新访问 Redis。
     */
    @Test
    void shouldReloadDirectLookupSnapshotAfterRetirement() {
        // Given
        ProbabilitySnapshotKey snapshotKey = new ProbabilitySnapshotKey(PROBABILITY_KEY, 7L);
        ProbabilitySnapshotPointer pointer = new ProbabilitySnapshotPointer(
                PROBABILITY_KEY, ProbabilityAlgorithmType.DIRECT_LOOKUP, 7L);
        String suffix = PROBABILITY_KEY + ":7";
        String rateRangeKey = Constants.RedisKeys.STRATEGY_RATE_RANGE_KEY + suffix;
        String rateTableKey = Constants.RedisKeys.STRATEGY_RATE_TABLE_KEY + suffix;
        when(redisService.getValue(rateRangeKey, Integer.class))
                .thenReturn(Optional.of(1), Optional.of(2));
        when(redisService.getHashEntries(rateTableKey, Integer.class, Long.class))
                .thenReturn(Map.of(0, 100_011L), Map.of(0, 100_012L));

        // When
        int initialRateRange = repository.getRateRange(snapshotKey);
        long initialAwardId = repository.getStrategyAwardAssemble(snapshotKey, 0);
        repository.retireProbabilitySnapshot(pointer);
        int reloadedRateRange = repository.getRateRange(snapshotKey);
        long reloadedAwardId = repository.getStrategyAwardAssemble(snapshotKey, 0);

        // Then
        assertEquals(1, initialRateRange);
        assertEquals(100_011L, initialAwardId);
        assertEquals(2, reloadedRateRange);
        assertEquals(100_012L, reloadedAwardId);
        verify(redisService, times(2)).getValue(rateRangeKey, Integer.class);
        verify(redisService, times(2)).getHashEntries(rateTableKey, Integer.class, Long.class);
    }

    /**
     * Given 一份二分查找快照，When 将快照标记为退役，Then 范围值和概率区间表使用相同宽限期过期。
     */
    @Test
    void shouldRetireBinarySearchProbabilitySnapshot() {
        // Given
        ProbabilitySnapshotPointer pointer = new ProbabilitySnapshotPointer(
                PROBABILITY_KEY, ProbabilityAlgorithmType.BINARY_SEARCH, 7L);
        String suffix = PROBABILITY_KEY + ":7";

        // When
        repository.retireProbabilitySnapshot(pointer);

        // Then
        verify(redisService).expire(Constants.RedisKeys.STRATEGY_RATE_RANGE_KEY + suffix,
                SNAPSHOT_RETIREMENT_GRACE_PERIOD);
        verify(redisService).expire(Constants.RedisKeys.STRATEGY_PROBABILITY_RANGE_TABLE_KEY + suffix,
                SNAPSHOT_RETIREMENT_GRACE_PERIOD);
    }

    /**
     * Given 一份 Alias Method 快照，When 将快照标记为退役，Then 只给完整 Alias 表设置宽限期。
     */
    @Test
    void shouldRetireAliasMethodProbabilitySnapshot() {
        // Given
        ProbabilitySnapshotPointer pointer = snapshotPointer(7L);
        String aliasTableKey = Constants.RedisKeys.STRATEGY_ALIAS_TABLE_KEY + PROBABILITY_KEY + ":7";

        // When
        repository.retireProbabilitySnapshot(pointer);

        // Then
        verify(redisService).expire(aliasTableKey, SNAPSHOT_RETIREMENT_GRACE_PERIOD);
        verify(redisService, times(1)).expire(anyString(), any(Duration.class));
    }

    /**
     * Given 直接查表快照的第一个 Key 设置过期时间失败，When 执行退役，Then 继续处理剩余 Key 且不影响主流程。
     */
    @Test
    void shouldContinueRetirementWhenOneSnapshotKeyFails() {
        // Given
        ProbabilitySnapshotPointer pointer = new ProbabilitySnapshotPointer(
                PROBABILITY_KEY, ProbabilityAlgorithmType.DIRECT_LOOKUP, 7L);
        String suffix = PROBABILITY_KEY + ":7";
        String rateRangeKey = Constants.RedisKeys.STRATEGY_RATE_RANGE_KEY + suffix;
        String rateTableKey = Constants.RedisKeys.STRATEGY_RATE_TABLE_KEY + suffix;
        doThrow(new RuntimeException("redis unavailable")).when(redisService)
                .expire(rateRangeKey, SNAPSHOT_RETIREMENT_GRACE_PERIOD);

        // When & Then
        assertDoesNotThrow(() -> repository.retireProbabilitySnapshot(pointer));
        verify(redisService).expire(rateRangeKey, SNAPSHOT_RETIREMENT_GRACE_PERIOD);
        verify(redisService).expire(rateTableKey, SNAPSHOT_RETIREMENT_GRACE_PERIOD);
    }

    /**
     * Given 缺少快照指针，When 请求退役，Then 在访问 Redis 前拒绝无效参数。
     */
    @Test
    void shouldRejectNullProbabilitySnapshotWhenRetiring() {
        assertThrows(NullPointerException.class, () -> repository.retireProbabilitySnapshot(null));
        verifyNoInteractions(redisService);
    }

    /**
     * Given 读取当前指针时 Redis 异常，When 发布快照，Then 向上抛出异常并释放发布锁。
     */
    @Test
    void shouldReleasePublishLockWhenPointerReadFails() {
        // Given
        ProbabilitySnapshotPointer pointer = snapshotPointer(2L);
        RuntimeException failure = new RuntimeException("redis unavailable");
        when(redisService.getValue(SNAPSHOT_POINTER_KEY, ProbabilitySnapshotPointer.class)).thenThrow(failure);

        // When
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> repository.publishProbabilitySnapshotPointer(pointer));

        // Then
        assertSame(failure, exception);
        verify(redisService).unlock(SNAPSHOT_PUBLISH_LOCK_KEY);
        verify(redisService, never()).setValue(SNAPSHOT_POINTER_KEY, pointer);
    }

    /**
     * Given Redis 中存在生效指针，When 按装配键查询，Then 返回完整版本和算法信息。
     */
    @Test
    void shouldGetActiveProbabilitySnapshotPointer() {
        // Given
        ProbabilitySnapshotPointer pointer = snapshotPointer(5L);
        when(redisService.getValue(SNAPSHOT_POINTER_KEY, ProbabilitySnapshotPointer.class))
                .thenReturn(Optional.of(pointer));

        // When
        Optional<ProbabilitySnapshotPointer> result = repository.getProbabilitySnapshotPointer(PROBABILITY_KEY);

        // Then
        assertEquals(Optional.of(pointer), result);
    }

    /**
     * Given 空白装配键，When 申请版本或读取指针，Then 在访问 Redis 前拒绝请求。
     */
    @Test
    void shouldRejectBlankProbabilitySnapshotKey() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> repository.nextProbabilitySnapshotVersion(" ")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> repository.getProbabilitySnapshotPointer(" "))
        );
        verifyNoInteractions(redisService);
    }

    /**
     * Given 一条库存扣减消息，When 发送库存消费消息，Then 发布奖品库存扣减事件。
     */
    @Test
    void shouldPublishAwardStockMessage() {
        // Given
        StrategyAwardStockKeyVO message = new StrategyAwardStockKeyVO(STRATEGY_ID, AWARD_ID, "user-001");

        // When
        stockMessageRepository.send(message);

        // Then
        ArgumentCaptor<AwardStockDeductedEvent> eventCaptor = ArgumentCaptor.forClass(AwardStockDeductedEvent.class);
        verify(messagePublisher).publish(eventCaptor.capture());
        assertAll(
                () -> assertEquals(message.strategyId(), eventCaptor.getValue().strategyId()),
                () -> assertEquals(message.awardId(), eventCaptor.getValue().awardId()),
                () -> assertEquals(message.userId(), eventCaptor.getValue().userId())
        );
        verify(redisService, never()).offerDelayed(anyString(), any(), any());
    }

    /**
     * Given MQ 发布失败，When 发送库存消费消息，Then 延迟三秒投递到补偿队列。
     */
    @Test
    void shouldFallbackAwardStockMessageToDelayedQueueWhenPublishFails() {
        // Given
        StrategyAwardStockKeyVO message = new StrategyAwardStockKeyVO(STRATEGY_ID, AWARD_ID, "user-001");
        doThrow(new RuntimeException("publish failed")).when(messagePublisher).publish(any(AwardStockDeductedEvent.class));

        // When
        stockMessageRepository.send(message);

        // Then
        verify(redisService).offerDelayed(
                Constants.RedisKeys.STRATEGY_AWARD_STOCK_QUEUE,
                message,
                Duration.ofSeconds(3)
        );
    }

    /**
     * Given 已到期的库存扣减消息，When 获取队列元素，Then 非阻塞返回类型安全的消息。
     */
    @Test
    void shouldPollAwardStockMessageFromDelayedQueue() {
        // Given
        StrategyAwardStockKeyVO expected = new StrategyAwardStockKeyVO(STRATEGY_ID, AWARD_ID, "user-001");
        when(redisService.pollDelayed(
                Constants.RedisKeys.STRATEGY_AWARD_STOCK_QUEUE,
                StrategyAwardStockKeyVO.class
        )).thenReturn(Optional.of(expected));

        // When
        Optional<StrategyAwardStockKeyVO> result = stockMessageRepository.poll();

        // Then
        assertEquals(Optional.of(expected), result);
    }

    /**
     * Given 数据库库存充足，When 同步一条库存消息，Then 返回成功更新结果。
     */
    @Test
    void shouldUpdateAwardStock() {
        // Given
        when(strategyAwardDao.decrementAwardCountSurplus(STRATEGY_ID, AWARD_ID)).thenReturn(1);

        // When
        boolean updated = repository.updateStrategyAwardStock(STRATEGY_ID, AWARD_ID);

        // Then
        assertTrue(updated);
    }

    /**
     * 构造奖品级次数锁持久化对象。
     *
     * @param awardId   奖品标识
     * @param ruleValue 次数锁规则值
     * @return 可供仓储查询测试使用的规则对象
     */
    private StrategyRulePO newLockRule(Long awardId, String ruleValue) {
        StrategyRulePO rule = new StrategyRulePO();
        rule.setStrategyId(STRATEGY_ID);
        rule.setAwardId(awardId);
        rule.setRuleModel(RuleModel.LOCK.getCode());
        rule.setRuleValue(ruleValue);
        return rule;
    }

    /**
     * 构造仓储读写测试使用的两列 Alias 概率表。
     *
     * @return 包含两个奖品列的不可变 Alias 概率表
     */
    private AliasProbabilityTable aliasProbabilityTable() {
        return new AliasProbabilityTable(List.of(100_011L, 100_012L), new double[]{0.5D, 1D}, new int[]{1, 1});
    }

    /**
     * 构造仓储发布测试使用的 Alias Method 快照指针。
     *
     * @param version 快照版本
     * @return 使用固定装配键和 Alias Method 算法的指针
     */
    private ProbabilitySnapshotPointer snapshotPointer(long version) {
        return new ProbabilitySnapshotPointer(PROBABILITY_KEY, ProbabilityAlgorithmType.ALIAS_METHOD, version);
    }
}
