package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.strategy.model.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.infrastructure.persistent.dao.IStrategyAwardDao;
import com.lavyoung.marketforge.infrastructure.persistent.mapper.StrategyAwardMapper;
import com.lavyoung.marketforge.infrastructure.persistent.redis.IRedisService;
import com.lavyoung.marketforge.types.common.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 策略仓库实现
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyRepository implements IStrategyRepository {

    private final IStrategyAwardDao strategyAwardDao;

    private final IRedisService redisService;

    private final StrategyAwardMapper strategyAwardMapper;

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
     * @param strategyId                       策略标识
     * @param rateRange                        概率查找范围
     * @param shuffleStrategyAwardSearchTables 下标到奖品标识的乱序查找表
     */
    @Override
    public void storeStrategyAwardSearchTables(Long strategyId, Integer rateRange, Map<Integer, Long> shuffleStrategyAwardSearchTables) {
        // 1. 存储抽奖策略范围值 如随机数的范围
        redisService.setValue(Constants.RedisKeys.STRATEGY_RATE_RANGE_KEY + strategyId, rateRange);
        // 2. 存储概率查找表
        redisService.putHashValues(Constants.RedisKeys.STRATEGY_RATE_TABLE_KEY + strategyId, shuffleStrategyAwardSearchTables);
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
     * @param strategyId 策略标识
     * @param rateKey    概率查找表下标
     * @return 奖品标识；查找表中不存在对应下标时返回 {@code 0}
     */
    @Override
    public long getStrategyAwardAssemble(Long strategyId, int rateKey) {
        return redisService.getHashValue(Constants.RedisKeys.STRATEGY_RATE_TABLE_KEY + strategyId, rateKey, Long.class).orElse(0L);
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
