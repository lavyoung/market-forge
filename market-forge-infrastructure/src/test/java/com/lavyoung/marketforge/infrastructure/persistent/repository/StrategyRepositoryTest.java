package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.lavyoung.marketforge.domain.strategy.event.AwardStockDeductedEvent;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardStockKeyVO;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilityAlgorithmType;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityRange;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.AwardAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.StrategyAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.StrategyAwardAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.StrategyRuleAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.*;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyAwardPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyRulePO;
import com.lavyoung.marketforge.infrastructure.persistent.redis.IRedisService;
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
    private static final int TOTAL_WEIGHT = 100;
    private static final Duration DEFAULT_STOCK_LOCK_LEASE_TIME = Duration.ofSeconds(30);

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
        repository = new StrategyRepository(
                strategyAwardDao,
                strategyDao,
                strategyRuleDao,
                userAwardRecordDao,
                awardDao,
                redisService,
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
}
