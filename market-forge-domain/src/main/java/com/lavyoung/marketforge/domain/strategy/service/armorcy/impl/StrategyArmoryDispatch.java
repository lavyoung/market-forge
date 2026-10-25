package com.lavyoung.marketforge.domain.strategy.service.armorcy.impl;

import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.WeightRuleValueVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.WeightTierVO;
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
     *
     * <p>每次重建先申请策略级单调递增代次，再读取数据库配置。普通奖池和该策略下的
     * 所有权重奖池共享同一代次，确保并发重建时较早发起的旧任务无法覆盖较新的结果。</p>
     *
     * @param strategyId 策略标识
     * @return 策略及其权重规则全部装配完成时返回 {@code true}
     * @throws NullPointerException strategyId 为空时抛出
     * @throws ArithmeticException  奖品概率无法形成有效概率范围时抛出
     * @throws RuntimeException     代次申请、配置读取或快照发布失败时抛出
     */
    @Override
    public boolean assembleLotteryStrategy(Long strategyId) {
        Objects.requireNonNull(strategyId, "strategyId must not be null");

        String assembleKey = String.valueOf(strategyId);
        // 必须在读取数据库前申请代次，保证较早发起的重建始终持有较小版本。
        long generation = repository.nextProbabilitySnapshotVersion(assembleKey);
        List<StrategyAwardEntity> strategyAwards = repository.queryStrategyAwardList(strategyId);
        initializeAwardStocks(strategyAwards);
        assembleProbabilitySpace(assembleKey, strategyAwards, generation);
        assembleWeightProbabilitySpaces(strategyId, strategyAwards, generation);
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
     */
    @Override
    public long getRandomAwardIdAndWeight(Long strategyId, String weightThreshold) {
        Objects.requireNonNull(strategyId, "strategyId must not be null");
        if (weightThreshold == null || weightThreshold.isBlank()) {
            throw new IllegalArgumentException("weightThreshold must not be blank");
        }
        return dispatch(strategyId + Constants.UNDERLINE + weightThreshold);
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
     * 使用指定策略代次建立概率空间，并在算法数据完整写入后发布生效指针。
     *
     * <p>代次由装配入口在读取数据库配置前统一申请，本方法不得再次生成版本。
     * 这样普通奖池和所有权重奖池可以属于同一次配置重建。</p>
     *
     * <ol>
     *     <li>根据奖品配置建立统一整数概率空间。</li>
     *     <li>根据空间规模选择概率算法。</li>
     *     <li>使用策略级代次构造候选快照。</li>
     *     <li>完整写入候选快照的算法数据。</li>
     *     <li>原子发布候选快照指针。</li>
     *     <li>发布失败时延迟退役候选快照。</li>
     * </ol>
     *
     * @param assembleKey    普通策略或权重奖池的装配键
     * @param strategyAwards 参与当前概率空间的奖品配置
     * @param generation     在读取配置前申请的策略重建代次
     * @throws BusinessException 概率空间无效或没有可用算法时抛出
     * @throws RuntimeException  快照写入或指针发布失败时抛出
     */
    private void assembleProbabilitySpace(String assembleKey, List<StrategyAwardEntity> strategyAwards, long generation) {
        StrategyProbabilitySpace probabilitySpace = probabilitySpaceFactory.create(assembleKey, strategyAwards);
        IProbabilityAlgorithm algorithm = probabilityAlgorithmRegistry.select(probabilitySpace);
        ProbabilitySnapshotPointer pointer = new ProbabilitySnapshotPointer(
                probabilitySpace.assembleKey(), algorithm.type(), generation);

        try {
            algorithm.assemble(probabilitySpace, pointer.snapshotKey());
        } catch (RuntimeException exception) {
            retireCandidateSnapshot(pointer, "概率快照装配失败");
            throw exception;
        }

        boolean published = repository.publishProbabilitySnapshotPointer(pointer);
        if (!published) {
            retireCandidateSnapshot(pointer, "概率快照版本已过期");
            log.info("忽略已经过期的概率快照发布 assembleKey: {} algorithm: {} generation: {}",
                    assembleKey, algorithm.type().getCode(), generation);
            return;
        }

        log.info("抽奖策略概率快照发布完成 assembleKey: {} algorithm: {} generation: {} totalWeight: {} awardCount: {}",
                assembleKey, algorithm.type().getCode(), generation,
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
     * <p>没有配置权重规则时不进行额外装配。规则存在时使用统一值对象解析档位，
     * 每个档位按照“策略标识_次数门槛”建立独立概率快照。</p>
     *
     * @param strategyId     策略标识
     * @param strategyAwards 策略包含的全部奖品
     * @param generation     本次策略配置重建的统一代次
     * @throws BusinessException 权重规则缺失、格式非法或档位没有可用奖品时抛出
     */
    private void assembleWeightProbabilitySpaces(Long strategyId, List<StrategyAwardEntity> strategyAwards, long generation) {
        StrategyEntity strategy = repository.queryStrategyEntityByStrategyId(strategyId);
        if (strategy == null || !strategy.toRuleModes().contains(RuleModel.WEIGHT)) {
            return;
        }

        StrategyRuleEntity weightRule = repository.getStrategyRule(strategyId, RuleModel.WEIGHT);
        if (weightRule == null) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_RULE_VALUE_INVALID, strategyId, RuleModel.WEIGHT.getCode());
        }

        WeightRuleValueVO ruleValue;
        try {
            ruleValue = WeightRuleValueVO.parse(weightRule.ruleValue());
        } catch (IllegalArgumentException exception) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_RULE_VALUE_INVALID, exception, strategyId, RuleModel.WEIGHT.getCode(), weightRule.ruleValue());
        }
        for (WeightTierVO tier : ruleValue.tiers()) {
            Set<Long> eligibleAwardIds = Set.copyOf(tier.awardIds());
            List<StrategyAwardEntity> eligibleAwards = strategyAwards.stream()
                    .filter(award -> eligibleAwardIds.contains(award.awardId()))
                    .toList();
            String assembleKey = strategyId + Constants.UNDERLINE + tier.threshold();
            assembleProbabilitySpace(assembleKey, eligibleAwards, generation);
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
