package com.lavyoung.marketforge.domain.strategy.service.armorcy.impl;

import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyArmory;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyDispatch;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.IProbabilityAlgorithm;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilityAlgorithmRegistry;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilityAlgorithmType;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilitySpaceFactory;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.StrategyProbabilitySpace;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 抽奖策略装配与派发领域服务。
 *
 * <p>装配阶段负责初始化奖品库存、建立整数概率空间、选择合适的概率算法，
 * 并保存算法类型。派发阶段读取装配元数据，使用与装配阶段一致的算法选取奖品。</p>
 *
 * <p>普通策略使用策略标识作为装配键；权重策略使用“策略标识_权重档位”
 * 作为装配键，两类概率空间可以独立选择算法和保存缓存数据。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyArmoryDispatch implements IStrategyArmory, IStrategyDispatch {

    private final IStrategyRepository repository;
    private final ProbabilitySpaceFactory probabilitySpaceFactory;
    private final ProbabilityAlgorithmRegistry probabilityAlgorithmRegistry;

    /**
     * {@inheritDoc}
     * <p>
     * 该实现按最小中奖概率计算查找表范围，将奖品标识按概率填充并乱序后写入仓储。
     *
     * @param strategyId 策略标识
     * @throws ArithmeticException 奖品概率无法形成有效概率范围时抛出
     */
    @Override
    public boolean assembleLotteryStrategy(Long strategyId) {
        Objects.requireNonNull(strategyId, "strategyId must not be null");
        List<StrategyAwardEntity> strategyAwards = repository.queryStrategyAwardList(strategyId);
        initializeAwardStocks(strategyAwards);
        assembleProbabilitySpace(String.valueOf(strategyId), strategyAwards);
        assembleWeightProbabilitySpaces(strategyId, strategyAwards);
        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @param strategyId 策略标识
     * @return 随机选中的奖品标识
     * @throws IllegalArgumentException 策略尚未装配或随机数范围无效时抛出
     */
    @Override
    public long getRandomAwardId(Long strategyId) {
        Objects.requireNonNull(strategyId, "strategyId must not be null");
        return dispatch(String.valueOf(strategyId));
    }

    /**
     * {@inheritDoc}
     * <p>
     * 使用“策略标识_权重规则值”作为装配键，从相应的权重概率表中查询奖品。
     *
     * @param strategyId      策略标识
     * @param ruleWeightValue 当前命中的权重规则值
     * @return 随机选中的奖品标识
     * @throws IllegalArgumentException 策略尚未装配或随机数范围无效时抛出
     */
    @Override
    public long getRandomAwardIdAndWeight(Long strategyId, String ruleWeightValue) {
        Objects.requireNonNull(strategyId, "strategyId must not be null");
        if (ruleWeightValue == null || ruleWeightValue.isBlank()) {
            throw new IllegalArgumentException("ruleWeightValue must not be blank");
        }
        return dispatch(strategyId + Constants.UNDERLINE + ruleWeightValue);
    }

    @Override
    public boolean subtractAwardStock(Long strategyId, Long awardId, LocalDateTime endDateTime) {
        return repository.subtractAwardStock(strategyId, awardId, 1, endDateTime);
    }

    /**
     * 使用数据库剩余库存初始化每个策略奖品的 Redis 库存。
     *
     * @param strategyAwards 参与当前策略的奖品配置
     */
    private void initializeAwardStocks(List<StrategyAwardEntity> strategyAwards) {
        for (StrategyAwardEntity strategyAward : strategyAwards) {
            repository.cacheStrategyAwardStock(strategyAward.strategyId(), strategyAward.awardId(), strategyAward.awardCountSurplus());
        }
    }

    /**
     * 建立整数概率空间并交给最合适的算法完成装配。
     *
     * <p>算法类型必须在算法数据写入成功之后保存。这样算法装配失败时不会
     * 留下指向不完整数据的新元数据。</p>
     *
     * @param assembleKey    策略装配键
     * @param strategyAwards 参与当前概率空间的奖品
     */
    private void assembleProbabilitySpace(String assembleKey, List<StrategyAwardEntity> strategyAwards) {
        StrategyProbabilitySpace probabilitySpace = probabilitySpaceFactory.create(assembleKey, strategyAwards);
        IProbabilityAlgorithm algorithm = probabilityAlgorithmRegistry.select(probabilitySpace);

        algorithm.assemble(probabilitySpace);
        repository.storeStrategyProbabilityAlgorithm(probabilitySpace.assembleKey(), algorithm.type());
        log.info("抽奖策略概率空间装配完成 assembleKey: {} algorithm: {} totalWeight: {} awardCount: {}",
                assembleKey, algorithm.type().getCode(), probabilitySpace.totalWeight(), probabilitySpace.awardWeights().size());
    }

    /**
     * 装配策略下的全部权重概率空间。
     *
     * <p>没有配置权重规则时不进行额外装配。每个权重档位只保留规则声明的
     * 奖品，并使用独立装配键选择算法和保存概率数据。</p>
     *
     * @param strategyId     策略标识
     * @param strategyAwards 策略包含的全部奖品
     * @throws BusinessException 策略声明权重规则但无法查询到规则配置时抛出
     */
    private void assembleWeightProbabilitySpaces(Long strategyId, List<StrategyAwardEntity> strategyAwards) {
        StrategyEntity strategy = repository.queryStrategyEntityByStrategyId(strategyId);
        if (strategy == null || !strategy.toRuleModes().contains(RuleModel.WEIGHT)) {
            return;
        }

        StrategyRuleEntity weightRule = repository.getStrategyRule(strategyId, RuleModel.WEIGHT);
        if (weightRule == null) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_RULE_VALUE_INVALID, strategyId, RuleModel.WEIGHT.getCode());
        }

        for (Map.Entry<String, List<Long>> entry : weightRule.ruleWeightValues().entrySet()) {
            Set<Long> eligibleAwardIds = Set.copyOf(entry.getValue());
            List<StrategyAwardEntity> eligibleAwards = strategyAwards.stream()
                    .filter(award -> eligibleAwardIds.contains(award.awardId()))
                    .toList();
            String assembleKey = strategyId + Constants.UNDERLINE + entry.getKey();
            assembleProbabilitySpace(assembleKey, eligibleAwards);
        }
    }

    /**
     * 根据装配阶段保存的算法类型派发奖品。
     *
     * @param assembleKey 策略装配键
     * @return 概率算法选中的奖品标识
     * @throws BusinessException 策略尚未装配或对应算法实现未注册时抛出
     */
    private long dispatch(String assembleKey) {
        ProbabilityAlgorithmType algorithmType = repository.getStrategyProbabilityAlgorithm(assembleKey)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.STRATEGY_NOT_ASSEMBLED, assembleKey));

        return probabilityAlgorithmRegistry.getRequired(algorithmType).dispatch(assembleKey);
    }

}
