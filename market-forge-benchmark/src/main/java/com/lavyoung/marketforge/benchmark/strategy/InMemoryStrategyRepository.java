package com.lavyoung.marketforge.benchmark.strategy;

import com.lavyoung.marketforge.domain.strategy.model.entity.AwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyAwardRuleModelVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilityAlgorithmType;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AliasProbabilityTable;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityRange;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;

import java.time.LocalDateTime;
import java.util.*;

/**
 * JMH 基准使用的内存策略仓储。
 *
 * <p>只实现概率算法装配和派发实际依赖的存储协议，避免 Redis 网络、序列化和
 * Mockito 调用成本污染算法基准。其余领域仓储能力一旦被误用会立即抛出异常。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
final class InMemoryStrategyRepository implements IStrategyRepository {

    private final Map<String, Integer> totalWeights = new HashMap<>();
    private final Map<String, Map<Integer, Long>> lookupTables = new HashMap<>();
    private final Map<String, List<AwardProbabilityRange>> probabilityRanges = new HashMap<>();
    private final Map<String, ProbabilityAlgorithmType> algorithmTypes = new HashMap<>();
    private final Map<String, AliasProbabilityTable> aliasTables = new HashMap<>();

    @Override
    public List<StrategyAwardEntity> queryStrategyAwardList(Long strategyId) {
        throw unsupported();
    }

    @Override
    public Optional<StrategyAwardEntity> getStrategyAwardEntity(Long strategyId, Long awardId) {
        throw unsupported();
    }

    @Override
    public void storeStrategyAwardSearchTables(String key, Integer rateRange, Map<Integer, Long> shuffleStrategyAwardSearchTables) {
        totalWeights.put(key, rateRange);
        lookupTables.put(key, shuffleStrategyAwardSearchTables);
    }

    @Override
    public int getRateRange(String key) {
        return totalWeights.getOrDefault(key, 0);
    }

    @Override
    public long getStrategyAwardAssemble(String key, int rateKey) {
        return lookupTables.getOrDefault(key, Map.of()).getOrDefault(rateKey, 0L);
    }

    @Override
    public void storeStrategyAwardProbabilityRanges(String key, int totalWeight, List<AwardProbabilityRange> ranges) {
        totalWeights.put(key, totalWeight);
        probabilityRanges.put(key, ranges);
    }

    @Override
    public List<AwardProbabilityRange> getStrategyAwardProbabilityRanges(String key) {
        return probabilityRanges.getOrDefault(key, List.of());
    }

    @Override
    public void storeStrategyAwardAliasTable(String key, AliasProbabilityTable aliasTable) {
        validateAliasTableKey(key);
        aliasTables.put(key, Objects.requireNonNull(aliasTable, "aliasTable must not be null"));
    }

    @Override
    public Optional<AliasProbabilityTable> getStrategyAwardAliasTable(String key) {
        validateAliasTableKey(key);
        return Optional.ofNullable(aliasTables.get(key));
    }

    @Override
    public void storeStrategyProbabilityAlgorithm(String key, ProbabilityAlgorithmType algorithmType) {
        algorithmTypes.put(key, algorithmType);
    }

    @Override
    public Optional<ProbabilityAlgorithmType> getStrategyProbabilityAlgorithm(String key) {
        return Optional.ofNullable(algorithmTypes.get(key));
    }

    @Override
    public StrategyEntity queryStrategyEntityByStrategyId(Long strategyId) {
        throw unsupported();
    }

    @Override
    public StrategyRuleEntity getStrategyRule(Long strategyId, RuleModel ruleModel) {
        throw unsupported();
    }

    @Override
    public String queryStrategyRuleValue(Long strategyId, Long awardId, String ruleModel) {
        throw unsupported();
    }

    @Override
    public String queryStrategyRuleValue(Long strategyId, String ruleModel) {
        throw unsupported();
    }

    @Override
    public StrategyAwardRuleModelVO queryStrategyAwardRuleModels(Long strategyId, long awardId) {
        throw unsupported();
    }

    @Override
    public void cacheStrategyAwardStock(Long strategyId, Long awardId, int stock) {
        throw unsupported();
    }

    @Override
    public boolean subtractAwardStock(Long strategyId, Long awardId, int stock, LocalDateTime endDateTime) {
        throw unsupported();
    }

    @Override
    public boolean updateStrategyAwardStock(Long strategyId, Long awardId) {
        throw unsupported();
    }

    @Override
    public long queryUserRaffleCount(String userId, Long strategyId) {
        throw unsupported();
    }

    @Override
    public Optional<AwardEntity> queryAwardEntity(Long awardId) {
        throw unsupported();
    }

    @Override
    public Map<Long, Integer> queryAwardRuleLockCounts(Long strategyId, List<Long> awardIds) {
        throw unsupported();
    }

    /**
     * 获取指定概率空间当前保存的元素数量，防止装配结果被 JIT 视为无效计算。
     *
     * @param key 概率空间装配键
     * @return 直接查找表或累计区间表的元素数量
     */
    int storedElementCount(String key) {
        Map<Integer, Long> lookupTable = lookupTables.get(key);
        if (lookupTable != null) {
            return lookupTable.size();
        }
        List<AwardProbabilityRange> ranges = probabilityRanges.get(key);
        if (ranges != null) {
            return ranges.size();
        }
        return Optional.ofNullable(aliasTables.get(key)).map(AliasProbabilityTable::size).orElse(0);
    }

    private UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("The benchmark repository only supports probability algorithm storage");
    }

    /**
     * 校验 JMH 内存仓储使用的 Alias 表装配键。
     *
     * @param key 普通策略或权重奖池使用的策略装配键
     * @throws IllegalArgumentException 装配键为空白时抛出
     */
    private void validateAliasTableKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be blank");
        }
    }

}
