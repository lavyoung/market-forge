package com.lavyoung.marketforge.domain.strategy.service.armorcy.impl;

import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyArmory;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyDispatch;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.IProbabilityAlgorithm;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilityAlgorithmRegistry;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilitySpaceFactory;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.ProbabilitySnapshotPointer;
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
 * 将完整数据写入新版本后发布生效指针。派发阶段只读取一次指针，并固定使用
 * 指针指定的算法和版本完成当前抽奖。</p>
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
     * 建立整数概率空间，将算法数据写入新版本并发布生效指针。
     *
     * <p>完整装配流程如下：</p>
     * <ol>
     *     <li>根据奖品配置建立统一的整数概率空间。</li>
     *     <li>根据概率空间规模选择直接查表、二分查找或 Alias Method。</li>
     *     <li>从仓储申请单调递增的快照版本。</li>
     *     <li>将算法数据写入带版本号的 Redis Key。</li>
     *     <li>算法数据全部写入成功后，原子发布新的生效指针。</li>
     *     <li>若装配失败或候选版本发布失败，则将候选快照标记为退役。</li>
     * </ol>
     *
     * <p>候选快照退役只设置延迟过期时间，不立即删除数据。这样既能清理装配失败
     * 产生的残留数据，也能避免误伤可能仍在读取旧版本的在途请求。</p>
     *
     * <p>如果发布操作抛出异常，则不在此处退役候选快照。因为此时无法判断指针
     * 是否已经成功切换，贸然退役可能删除刚刚生效的快照。</p>
     *
     * @param assembleKey 普通策略或权重奖池的装配键
     * @param strategyAwards 参与当前概率空间的奖品配置
     * @throws BusinessException 概率空间无效或没有可用算法时抛出
     * @throws RuntimeException 快照数据写入或指针发布失败时抛出
     */
    private void assembleProbabilitySpace(String assembleKey, List<StrategyAwardEntity> strategyAwards) {
        StrategyProbabilitySpace probabilitySpace = probabilitySpaceFactory.create(assembleKey, strategyAwards);
        IProbabilityAlgorithm algorithm = probabilityAlgorithmRegistry.select(probabilitySpace);
        long version = repository.nextProbabilitySnapshotVersion(probabilitySpace.assembleKey());
        ProbabilitySnapshotPointer pointer = new ProbabilitySnapshotPointer(
                probabilitySpace.assembleKey(), algorithm.type(), version);

        try {
            algorithm.assemble(probabilitySpace, pointer.snapshotKey());
        } catch (RuntimeException exception) {
            retireCandidateSnapshot(pointer, "概率快照装配失败");
            throw exception;
        }

        boolean published = repository.publishProbabilitySnapshotPointer(pointer);
        if (!published) {
            retireCandidateSnapshot(pointer, "概率快照版本已过期");
            log.info("忽略已经过期的概率快照发布 assembleKey: {} algorithm: {} version: {}",
                    assembleKey, algorithm.type().getCode(), version);
            return;
        }

        log.info("抽奖策略概率快照发布完成 assembleKey: {} algorithm: {} version: {} totalWeight: {} awardCount: {}",
                assembleKey, algorithm.type().getCode(), version,
                probabilitySpace.totalWeight(), probabilitySpace.awardWeights().size());
    }

    /**
     * 以尽力而为方式退役未生效的候选快照。
     *
     * <p>快照清理属于补偿行为，不能覆盖装配失败等原始业务异常。即使 Redis
     * 暂时不可用导致退役失败，也只记录日志并保留原有流程结果。</p>
     *
     * @param pointer 等待退役的候选快照指针
     * @param reason  触发退役的业务原因，用于问题排查
     */
    private void retireCandidateSnapshot(ProbabilitySnapshotPointer pointer, String reason) {
        try {
            repository.retireProbabilitySnapshot(pointer);
        } catch (RuntimeException exception) {
            log.warn("{}，候选快照退役失败 assembleKey: {} algorithm: {} version: {}",
                    reason, pointer.assembleKey(), pointer.algorithmType().getCode(), pointer.version(), exception);
        }
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
     * 根据当前生效快照指针选择算法并派发奖品。
     *
     * <p>一次抽奖只读取一次指针。即使装配线程随后发布新版本，当前请求仍然
     * 使用已经读取到的旧版本完成抽奖，不会在请求中途切换概率数据。</p>
     *
     * @param assembleKey 普通策略或权重奖池的装配键
     * @return 当前生效算法选中的奖品标识
     * @throws BusinessException 当前没有生效快照或对应算法未注册时抛出
     */
    private long dispatch(String assembleKey) {
        ProbabilitySnapshotPointer pointer = repository.getProbabilitySnapshotPointer(assembleKey)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.STRATEGY_NOT_ASSEMBLED, assembleKey));

        return probabilityAlgorithmRegistry.getRequired(pointer.algorithmType()).dispatch(pointer.snapshotKey());
    }

}
