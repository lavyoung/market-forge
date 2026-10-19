package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lavyoung.marketforge.domain.strategy.model.entity.AwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilityAlgorithmType;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityRange;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.AwardAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.StrategyAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.StrategyAwardAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.StrategyRuleAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.*;
import com.lavyoung.marketforge.infrastructure.persistent.po.AwardPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyAwardPO;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyRulePO;
import com.lavyoung.marketforge.infrastructure.persistent.po.UserAwardRecordPO;
import com.lavyoung.marketforge.infrastructure.persistent.redis.IRedisService;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 抽奖策略仓储端口的基础设施实现。
 * <p>
 * 使用 MyBatis/MyBatis-Plus 读取策略配置，并使用 Redis 保存策略缓存和装配后的概率查找表。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyRepository implements IStrategyRepository {

    private final IStrategyAwardDao strategyAwardDao;
    private final IStrategyDao strategyDao;
    private final IStrategyRuleDao strategyRuleDao;
    private final IUserAwardRecordDao userAwardRecordDao;
    private final IAwardDao awardDao;

    private final IRedisService redisService;

    private final StrategyAwardAssembler strategyAwardAssembler;
    private final StrategyAssembler strategyAssembler;
    private final StrategyRuleAssembler strategyRuleAssembler;
    private final AwardAssembler awardAssembler;

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

    @Override
    public Optional<StrategyAwardEntity> getStrategyAwardEntity(Long strategyId, Long awardId) {
        return Optional.ofNullable(strategyAwardDao.selectOne(Wrappers.lambdaQuery(StrategyAwardPO.class)
                .eq(StrategyAwardPO::getStrategyId, strategyId)
                .eq(StrategyAwardPO::getAwardId, awardId))).map(strategyAwardAssembler::toEntity);
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
     * @param key 策略装配键
     * @return 随机数范围；策略尚未装配时返回 {@code 0}
     */
    @Override
    public int getRateRange(String key) {
        return redisService.getValue(Constants.RedisKeys.STRATEGY_RATE_RANGE_KEY + key, Integer.class).orElse(0);
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

    @Override
    public void storeStrategyAwardProbabilityRanges(String key, int totalWeight, List<AwardProbabilityRange> probabilityRanges) {
        redisService.setValue(Constants.RedisKeys.STRATEGY_RATE_RANGE_KEY + key, totalWeight);
        redisService.setValue(Constants.RedisKeys.STRATEGY_PROBABILITY_RANGE_TABLE_KEY + key, List.copyOf(probabilityRanges));
    }

    @Override
    public List<AwardProbabilityRange> getStrategyAwardProbabilityRanges(String key) {
        return redisService.getValueList(Constants.RedisKeys.STRATEGY_PROBABILITY_RANGE_TABLE_KEY + key,
                AwardProbabilityRange.class).orElseGet(List::of);
    }

    @Override
    public void storeStrategyProbabilityAlgorithm(String key, ProbabilityAlgorithmType algorithmType) {
        Objects.requireNonNull(algorithmType, "algorithmType must not be null");
        redisService.setValue(Constants.RedisKeys.STRATEGY_PROBABILITY_ALGORITHM_KEY + key, algorithmType.getCode());
    }

    @Override
    public Optional<ProbabilityAlgorithmType> getStrategyProbabilityAlgorithm(String key) {
        return redisService.getValue(Constants.RedisKeys.STRATEGY_PROBABILITY_ALGORITHM_KEY + key,
                String.class).flatMap(ProbabilityAlgorithmType::fromCode);
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
        String cacheKey = Constants.RedisKeys.STRATEGY_KEY + strategyId;
        return redisService.getValue(cacheKey, StrategyEntity.class).orElseGet(() -> {
            StrategyEntity entity = strategyAssembler.toEntity(
                    strategyDao.queryStrategyByStrategyId(strategyId).orElse(null)
            );
            if (entity != null) {
                redisService.setValue(cacheKey, entity);
            }
            return entity;
        });
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
                .eq(StrategyRulePO::getRuleModel, ruleModel.getCode())
        )).stream().map(strategyRuleAssembler::toEntity).findFirst().orElse(null);
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
     * {@inheritDoc}
     * <p>
     * 委托奖品标识可为空的查询方法读取策略级规则值。
     *
     * @param strategyId 策略标识
     * @param ruleModel  规则模型编码
     * @return 规则配置值；未找到匹配规则时返回 {@code null}
     */
    @Override
    public String queryStrategyRuleValue(Long strategyId, String ruleModel) {
        return queryStrategyRuleValue(strategyId, null, ruleModel);
    }

    /**
     * {@inheritDoc}
     * <p>
     * 使用 MyBatis-Plus 按策略标识和奖品标识查询奖品配置，并将规则模型字段转换为值对象。
     *
     * @param strategyId 策略标识
     * @param awardId    奖品标识
     * @return 策略奖品规则模型值对象；未找到奖品配置时返回 {@code null}
     */
    @Override
    public StrategyAwardRuleModelVO queryStrategyAwardRuleModels(Long strategyId, long awardId) {
        StrategyAwardPO strategyRulePO = strategyAwardDao.selectOne(Wrappers.lambdaQuery(StrategyAwardPO.class)
                .eq(StrategyAwardPO::getStrategyId, strategyId)
                .eq(StrategyAwardPO::getAwardId, awardId));
        return Optional.ofNullable(strategyRulePO).map(x -> StrategyAwardRuleModelVO.builder().ruleModels(x.getRuleModels()).build()).orElse(null);
    }

    @Override
    public void cacheStrategyAwardStock(Long strategyId, Long awardId, int stock) {
        String key = Constants.RedisKeys.STRATEGY_AWARD_STOCK + strategyId + Constants.UNDERLINE + awardId;
        redisService.setAtomicLong(key, stock);
    }

    @Override
    public boolean subtractAwardStock(Long strategyId, Long awardId, int stock, LocalDateTime endDateTime) {
        Objects.requireNonNull(strategyId, "strategyId must not be null");
        Objects.requireNonNull(awardId, "awardId must not be null");
        String key = Constants.RedisKeys.STRATEGY_AWARD_STOCK + strategyId + Constants.UNDERLINE + awardId;
        if (key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        if (stock <= 0) {
            throw new IllegalArgumentException("stock must be greater than zero");
        }
        String lockKey = key + Constants.COLON + "lock";
        redisService.lock(lockKey, stockLockLeaseTime(endDateTime));
        try {
            long currentStock = redisService.getAtomicLong(key);
            if (currentStock < stock) {
                return false;
            }
            redisService.addAndGetAtomicLong(key, -stock);
            return true;
        } finally {
            redisService.unlock(lockKey);
        }
    }

    /**
     * 计算库存锁租约。
     * <p>
     * 活动未结束时，让锁的最长存活时间覆盖到活动结束后一日，避免服务宕机导致锁无限悬挂。
     * 旧调用没有传入活动结束时间时，使用较短租约兼容策略维度的直接扣减。
     *
     * @param endDateTime 活动结束时间，可为空
     * @return Redis 锁固定租约
     */
    private Duration stockLockLeaseTime(LocalDateTime endDateTime) {
        if (endDateTime == null) {
            return Duration.ofSeconds(30);
        }
        Duration leaseTime = Duration.between(LocalDateTime.now(), endDateTime.plusDays(1));
        if (leaseTime.isZero() || leaseTime.isNegative()) {
            return Duration.ofSeconds(1);
        }
        return leaseTime;
    }

    @Override
    public boolean updateStrategyAwardStock(Long strategyId, Long awardId) {
        return strategyAwardDao.decrementAwardCountSurplus(strategyId, awardId) == 1;
    }

    @Override
    public long queryUserRaffleCount(String userId, Long strategyId) {
        // 补真实次数锁 合理的是查活动订单或中奖记录： 查活动订单：代表用户已经消耗过多少次参与机会。  查中奖记录：代表用户已经完成过多少次有效抽奖结果
        return userAwardRecordDao.selectCount(Wrappers.lambdaQuery(UserAwardRecordPO.class)
                .eq(UserAwardRecordPO::getUserId, userId)
                .eq(UserAwardRecordPO::getStrategyId, strategyId));
    }

    @Override
    public Optional<AwardEntity> queryAwardEntity(Long awardId) {
        return Optional.ofNullable(awardAssembler.toEntity(awardDao.selectOne(Wrappers.lambdaQuery(AwardPO.class)
                .eq(AwardPO::getAwardId, awardId))));
    }

    @Override
    public Map<Long, Integer> queryAwardRuleLockCounts(Long strategyId, List<Long> awardIds) {
        Objects.requireNonNull(strategyId, "strategyId must not be null");
        Objects.requireNonNull(awardIds, "awardIds must not be null");
        List<Long> distinctAwardIds = awardIds.stream().filter(Objects::nonNull).distinct().toList();

        if (distinctAwardIds.isEmpty()) {
            return Map.of();
        }

        List<StrategyRulePO> lockRules = strategyRuleDao.selectList(Wrappers.lambdaQuery(StrategyRulePO.class)
                .eq(StrategyRulePO::getStrategyId, strategyId)
                .in(StrategyRulePO::getAwardId, distinctAwardIds)
                .eq(StrategyRulePO::getRuleModel, RuleModel.LOCK.getCode()));

        Map<Long, Integer> lockCountMap = lockRules.stream()
                .collect(Collectors.toUnmodifiableMap(StrategyRulePO::getAwardId,
                        this::parseAwardLockCount, (left, right) -> {
                            throw BusinessException.of(BusinessResponseCode.STRATEGY_RULE_VALUE_INVALID, strategyId, left, right);
                        }));

        if (lockCountMap.size() != distinctAwardIds.size()) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_RULE_NOT_FOUND, strategyId, distinctAwardIds);
        }

        return lockCountMap;
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
        List<StrategyAwardEntity> strategyAwards = strategyAwardAssembler.toEntities(
                strategyAwardDao.queryStrategyAwardList(strategyId)
        );
        redisService.setValue(cacheKey, strategyAwards);
        return strategyAwards;
    }

    /**
     * 解析奖品级次数锁规则值。
     *
     * @param rulePO 奖品级策略规则持久化对象
     * @return 非负解锁次数
     * @throws BusinessException 规则值为空、不是整数或小于零时抛出
     */
    private int parseAwardLockCount(StrategyRulePO rulePO) {
        String ruleValue = rulePO.getRuleValue();
        if (StringUtils.isBlank(ruleValue)) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_RULE_VALUE_INVALID, rulePO.getStrategyId(), rulePO.getAwardId(), ruleValue);
        }

        try {
            int lockCount = Integer.parseInt(ruleValue);
            if (lockCount < 0) {
                throw new NumberFormatException("negative lock count");
            }
            return lockCount;
        } catch (NumberFormatException exception) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_RULE_VALUE_INVALID, exception, rulePO.getStrategyId(), rulePO.getAwardId(), ruleValue);
        }
    }
}
