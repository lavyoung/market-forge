package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IStrategyAwardDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IStrategyDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IStrategyRuleDao;
import com.lavyoung.marketforge.infrastructure.persistent.mapper.StrategyAwardMapper;
import com.lavyoung.marketforge.infrastructure.persistent.mapper.StrategyMapper;
import com.lavyoung.marketforge.infrastructure.persistent.mapper.StrategyRuleMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyAwardPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyRulePO;
import com.lavyoung.marketforge.infrastructure.persistent.redis.IRedisService;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link StrategyRepository} 对策略级规则值和奖品规则模型的查询转换行为。
 */
@ExtendWith(MockitoExtension.class)
class StrategyRepositoryTest {

    private static final Long STRATEGY_ID = 100_001L;
    private static final long AWARD_ID = 100_011L;

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
}
