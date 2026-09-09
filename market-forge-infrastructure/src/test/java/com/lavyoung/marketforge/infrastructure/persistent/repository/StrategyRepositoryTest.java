package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardStockKeyVO;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IStrategyAwardDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IStrategyDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IStrategyRuleDao;
import com.lavyoung.marketforge.infrastructure.persistent.mapper.StrategyAwardMapper;
import com.lavyoung.marketforge.infrastructure.persistent.mapper.StrategyMapper;
import com.lavyoung.marketforge.infrastructure.persistent.mapper.StrategyRuleMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyAwardPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyRulePO;
import com.lavyoung.marketforge.infrastructure.persistent.redis.IRedisService;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
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

    @Mock
    private IStrategyAwardDao strategyAwardDao;

    @Mock
    private IStrategyDao strategyDao;

    @Mock
    private IStrategyRuleDao strategyRuleDao;

    @Mock
    private IRedisService redisService;

    @Mock
    private StrategyAwardMapper strategyAwardMapper;

    @Mock
    private StrategyMapper strategyMapper;

    @Mock
    private StrategyRuleMapper strategyRuleMapper;

    private StrategyRepository repository;

    /**
     * Given 模拟基础设施依赖，When 初始化仓储实现，Then 使用可控 DAO 返回值执行测试。
     */
    @BeforeEach
    void setUp() {
        repository = new StrategyRepository(
                strategyAwardDao,
                strategyDao,
                strategyRuleDao,
                redisService,
                strategyAwardMapper,
                strategyMapper,
                strategyRuleMapper
        );
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
     * Given Redis 库存充足，When 扣减奖品库存，Then 在分布式锁保护下完成原子扣减并释放锁。
     */
    @Test
    void shouldSubtractAwardStockWithinDistributedLock() {
        // Given
        when(redisService.getAtomicLong(STOCK_KEY)).thenReturn(10L);

        // When
        boolean subtracted = repository.subtractAwardStock(STOCK_KEY, 2);

        // Then
        assertTrue(subtracted);
        verify(redisService).lock(STOCK_LOCK_KEY);
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
        boolean subtracted = repository.subtractAwardStock(STOCK_KEY, 2);

        // Then
        assertFalse(subtracted);
        verify(redisService).lock(STOCK_LOCK_KEY);
        verify(redisService, never()).addAndGetAtomicLong(STOCK_KEY, -2L);
        verify(redisService).unlock(STOCK_LOCK_KEY);
    }

    /**
     * Given 非法库存参数，When 尝试扣减库存，Then 在访问 Redis 前拒绝请求。
     */
    @Test
    void shouldRejectInvalidAwardStockArguments() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> repository.subtractAwardStock(" ", 1)),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> repository.subtractAwardStock(STOCK_KEY, 0))
        );
        verify(redisService, never()).lock(STOCK_LOCK_KEY);
    }

    /**
     * Given 一条库存扣减消息，When 发送库存消费队列，Then 延迟三秒投递到库存阻塞队列。
     */
    @Test
    void shouldSendAwardStockMessageToDelayedQueue() {
        // Given
        StrategyAwardStockKeyVO message = new StrategyAwardStockKeyVO(STRATEGY_ID, AWARD_ID);

        // When
        repository.awardStockConsumeSendQueue(message);

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
        StrategyAwardStockKeyVO expected = new StrategyAwardStockKeyVO(STRATEGY_ID, AWARD_ID);
        when(redisService.pollDelayed(
                Constants.RedisKeys.STRATEGY_AWARD_STOCK_QUEUE,
                StrategyAwardStockKeyVO.class
        )).thenReturn(Optional.of(expected));

        // When
        Optional<StrategyAwardStockKeyVO> result = repository.pollQueueValue();

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
}
