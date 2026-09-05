package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IStrategyAwardDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IStrategyDao;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IStrategyRuleDao;
import com.lavyoung.marketforge.infrastructure.persistent.mapper.StrategyAwardMapper;
import com.lavyoung.marketforge.infrastructure.persistent.mapper.StrategyMapper;
import com.lavyoung.marketforge.infrastructure.persistent.mapper.StrategyRuleMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyRulePO;
import com.lavyoung.marketforge.infrastructure.persistent.redis.IRedisService;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 抽奖策略仓储端口的基础设施实现。
 * <p>
 * 使用 MyBatis/MyBatis-Plus 读取策略配置，并使用 Redis 保存策略缓存和装配后的概率查找表。
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyRepository implements IStrategyRepository {

    /**
     * 策略奖品数据访问对象。
     */
    private final IStrategyAwardDao strategyAwardDao;

    /**
     * 策略基础信息数据访问对象。
     */
    private final IStrategyDao strategyDao;

    /**
     * 策略规则数据访问对象。
     */
    private final IStrategyRuleDao strategyRuleDao;

    /**
     * Redis 缓存服务。
     */
    private final IRedisService redisService;

    /**
     * 策略奖品持久化对象转换器。
     */
    private final StrategyAwardMapper strategyAwardMapper;

    /**
     * 策略持久化对象转换器。
     */
    private final StrategyMapper strategyMapper;

    /**
     * 策略规则持久化对象转换器。
     */
    private final StrategyRuleMapper strategyRuleMapper;

    /**
     * {@inheritDoc}
     * <p>
     * 优先读取 Redis 缓存，缓存未命中时查询数据库并回填缓存。
     *
     * @param strategyId 策略标识
     * @return 策略奖品列表；不存在配置时返回空列表
     */
    @Override
    public List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId) {
        String cacheKey = Constants.RedisKeys.STRATEGY_AWARD_KEY + strategyId;
        return redisService.getValueList(cacheKey, StrategyAwardEntity.class)
                .orElseGet(() -> queryAndCacheStrategyAwards(strategyId, cacheKey));
    }

    /**
     * {@inheritDoc}
     *
     * @param key                              策略标识
     * @param rateRange                        概率查找范围
     * @param shuffleStrategyAwardSearchTables 下标到奖品标识的乱序查找表
     */
    @Override
    public void storeStrategyAwardSearchTables(String key, Integer rateRange, Map<Integer, Long> shuffleStrategyAwardSearchTables) {
        // 1. 存储抽奖策略范围值 如随机数的范围
        redisService.setValue(Constants.RedisKeys.STRATEGY_RATE_RANGE_KEY + key, rateRange);
        // 2. 存储概率查找表
        redisService.putHashValues(Constants.RedisKeys.STRATEGY_RATE_TABLE_KEY + key, shuffleStrategyAwardSearchTables);
    }

    /**
     * {@inheritDoc}
     *
     * @param strategyId 策略标识
     * @return 随机数范围；策略尚未装配时返回 {@code 0}
     */
    @Override
    public int getRateRange(Long strategyId) {
        return redisService.getValue(Constants.RedisKeys.STRATEGY_RATE_RANGE_KEY + strategyId, Integer.class).orElse(0);
    }

    /**
     * {@inheritDoc}
     *
     * @param key     策略装配键
     * @param rateKey 概率查找表下标
     * @return 奖品标识；缓存字段不存在时返回 {@code 0}
     */
    @Override
    public long getStrategyAwardAssemble(String key, int rateKey) {
        return redisService.getHashValue(Constants.RedisKeys.STRATEGY_RATE_TABLE_KEY + key, rateKey, Long.class).orElse(0L);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 优先读取 Redis 缓存，缓存未命中时查询数据库。
     *
     * @param strategyId 策略标识
     * @return 策略实体；策略不存在时返回 {@code null}
     */
    @Override
    public StrategyEntity queryStrategyEntityByStrategyId(Long strategyId) {
        // 缓存key
        String cacheKey = Constants.RedisKeys.STRATEGY_KEY + strategyId;
        return redisService.getValue(cacheKey, StrategyEntity.class)
                .orElseGet(() -> strategyMapper.toEntity(strategyDao.queryStrategyByStrategyId(strategyId).orElse(null)));
    }

    /**
     * {@inheritDoc}
     * <p>
     * 使用 MyBatis-Plus 条件构造器按策略标识和规则模型查询唯一规则。
     *
     * @param strategyId 策略标识
     * @param ruleModel  需要查询的规则模型
     * @return 策略规则实体；规则不存在时返回 {@code null}
     */
    @Override
    public StrategyRuleEntity getStrategyRule(Long strategyId, RuleModel ruleModel) {
        return Optional.ofNullable(strategyRuleDao.selectOne(Wrappers.lambdaQuery(StrategyRulePO.class)
                .eq(StrategyRulePO::getStrategyId, strategyId)
                .eq(StrategyRulePO::getRuleModel, ruleModel)
        )).stream().map(strategyRuleMapper::toEntity).findFirst().orElse(null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 使用 MyBatis-Plus 按策略、可选奖品标识和规则模型查询唯一规则值。
     *
     * @param strategyId 策略标识
     * @param awardId    奖品标识；查询策略级规则时可为空
     * @param ruleModel  规则模型编码
     * @return 规则配置值；未找到匹配规则时返回 {@code null}
     */
    @Override
    public String queryStrategyRuleValue(Long strategyId, Long awardId, String ruleModel) {
        return Optional.ofNullable(strategyRuleDao.selectOne(Wrappers.lambdaQuery(StrategyRulePO.class)
                .eq(StrategyRulePO::getStrategyId, strategyId)
                .eq(Objects.nonNull(awardId), StrategyRulePO::getAwardId, awardId)
                .eq(StrategyRulePO::getRuleModel, ruleModel))
        ).stream().map(StrategyRulePO::getRuleValue).findFirst().orElse(null);
    }

    /**
     * 查询数据库中的策略奖品，并将领域实体写入缓存。
     *
     * @param strategyId 策略标识
     * @param cacheKey   策略奖品缓存键
     * @return 策略奖品领域实体列表
     */
    private List<StrategyAwardEntity> queryAndCacheStrategyAwards(
            Long strategyId,
            String cacheKey) {
        List<StrategyAwardEntity> strategyAwards = strategyAwardMapper.toEntities(
                strategyAwardDao.queryStrategyAwardList(strategyId)
        );
        redisService.setValue(cacheKey, strategyAwards);
        return strategyAwards;
    }
}
