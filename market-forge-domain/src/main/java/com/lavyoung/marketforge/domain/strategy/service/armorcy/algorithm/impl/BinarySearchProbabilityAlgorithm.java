package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.impl;

import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.IProbabilityAlgorithm;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilityAlgorithmType;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityRange;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityWeight;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.ProbabilitySnapshotKey;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.StrategyProbabilitySpace;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * 基于累计概率区间的二分查找算法。
 *
 * <p>装配阶段按照奖品权重依次计算累计上界，每个奖品只保存一个累计区间，
 * 因此空间复杂度为 O(n)。抽奖阶段生成随机下标，通过二分查找定位第一个
 * 累计上界大于该下标的奖品，查询复杂度为 O(log n)。</p>
 *
 * <p>该算法不会按照总权重展开完整查找表，适合总权重很大、概率精度较高的
 * 策略，避免直接查表算法占用过多 JVM 和 Redis 内存。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
@Component
public class BinarySearchProbabilityAlgorithm implements IProbabilityAlgorithm {

    private final IStrategyRepository repository;
    private final RandomGenerator randomGenerator;

    /**
     * 创建生产环境使用的二分概率算法。
     *
     * @param repository 策略仓储端口
     * @throws NullPointerException 策略仓储为空时抛出
     */
    @Autowired
    public BinarySearchProbabilityAlgorithm(IStrategyRepository repository) {
        this(repository, new SecureRandom());
    }

    /**
     * 创建使用指定随机数生成器的二分概率算法。
     *
     * <p>该构造器保持包级可见，供同包单元测试注入可预测随机数，
     * 不作为领域层对外公开的构造协议。</p>
     *
     * @param repository      策略仓储端口
     * @param randomGenerator 随机数生成器
     * @throws NullPointerException 仓储或随机数生成器为空时抛出
     */
    BinarySearchProbabilityAlgorithm(IStrategyRepository repository, RandomGenerator randomGenerator) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.randomGenerator = Objects.requireNonNull(randomGenerator, "randomGenerator must not be null");
    }

    @Override
    public ProbabilityAlgorithmType type() {
        return ProbabilityAlgorithmType.BINARY_SEARCH;
    }

    @Override
    public boolean supports(StrategyProbabilitySpace probabilitySpace) {
        Objects.requireNonNull(probabilitySpace, "probabilitySpace must not be null");
        return true;
    }

    @Override
    public void assemble(StrategyProbabilitySpace probabilitySpace, ProbabilitySnapshotKey snapshotKey) {
        Objects.requireNonNull(probabilitySpace, "probabilitySpace must not be null");
        Objects.requireNonNull(snapshotKey, "snapshotKey must not be null");
        if (!probabilitySpace.assembleKey().equals(snapshotKey.assembleKey())) {
            throw new IllegalArgumentException("probabilitySpace and snapshotKey must use the same assembleKey");
        }
        List<AwardProbabilityRange> probabilityRanges = createProbabilityRanges(probabilitySpace);
        repository.storeStrategyAwardProbabilityRanges(snapshotKey, probabilitySpace.totalWeight(), probabilityRanges);
    }

    @Override
    public long dispatch(ProbabilitySnapshotKey snapshotKey) {
        Objects.requireNonNull(snapshotKey, "snapshotKey must not be null");
        int totalWeight = repository.getRateRange(snapshotKey);
        if (totalWeight <= 0) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_NOT_ASSEMBLED,
                    snapshotKey.assembleKey(), snapshotKey.version());
        }
        List<AwardProbabilityRange> probabilityRanges = repository.getStrategyAwardProbabilityRanges(snapshotKey);
        validateProbabilityRanges(snapshotKey.assembleKey(), totalWeight, probabilityRanges);
        int randomIndex = randomGenerator.nextInt(totalWeight);
        return findAwardId(snapshotKey.assembleKey(), randomIndex, probabilityRanges);
    }

    /**
     * 根据奖品整数权重生成累计概率区间。
     *
     * <p>例如三个奖品的权重依次为 {@code 6、3、1}，生成的累计上界为
     * {@code 6、9、10}，分别覆盖 {@code [0,6)、[6,9)、[9,10)}。</p>
     *
     * @param probabilitySpace 已完成整数化和完整性校验的概率空间
     * @return 按累计上界升序排列的不可变概率区间
     */
    private List<AwardProbabilityRange> createProbabilityRanges(StrategyProbabilitySpace probabilitySpace) {
        List<AwardProbabilityRange> probabilityRanges = new ArrayList<>(probabilitySpace.awardWeights().size());

        int cumulativeWeight = 0;

        for (AwardProbabilityWeight awardWeight : probabilitySpace.awardWeights()) {
            cumulativeWeight = Math.addExact(cumulativeWeight, awardWeight.weight());
            probabilityRanges.add(new AwardProbabilityRange(cumulativeWeight, awardWeight.awardId()));
        }

        return List.copyOf(probabilityRanges);
    }


    /**
     * 校验 Redis 中的累计概率区间具备可查询条件。
     *
     * <p>完整遍历会把每次抽奖退化为 O(n)，因此这里只执行 O(1) 校验：
     * 列表不能为空，并且最后一个累计上界必须等于随机数总权重。区间顺序和
     * 递增性由装配阶段负责保证。</p>
     *
     * @param assembleKey       策略装配键
     * @param totalWeight       随机数总权重
     * @param probabilityRanges 累计概率区间
     * @throws BusinessException 区间为空或没有完整覆盖概率空间时抛出
     */
    private void validateProbabilityRanges(String assembleKey, int totalWeight, List<AwardProbabilityRange> probabilityRanges) {
        if (probabilityRanges.isEmpty()) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_ASSEMBLY_FAILED, assembleKey, "probability ranges are empty");
        }

        int finalUpperBound = probabilityRanges.get(probabilityRanges.size() - 1).upperBoundExclusive();
        if (finalUpperBound != totalWeight) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_ASSEMBLY_FAILED, assembleKey, totalWeight, finalUpperBound);
        }
    }

    /**
     * 使用二分查找定位随机下标对应的奖品。
     *
     * @param assembleKey       策略装配键
     * @param randomIndex       位于 {@code [0, totalWeight)} 的随机下标
     * @param probabilityRanges 按累计上界升序排列的概率区间
     * @return 命中的奖品标识
     * @throws BusinessException 区间顺序损坏或随机下标未落入目标区间时抛出
     */
    private long findAwardId(String assembleKey, int randomIndex, List<AwardProbabilityRange> probabilityRanges) {
        int left = 0;
        int right = probabilityRanges.size() - 1;

        while (left < right) {
            int middle = left + (right - left) / 2;
            if (randomIndex < probabilityRanges.get(middle).upperBoundExclusive()) {
                right = middle;
            } else {
                left = middle + 1;
            }
        }

        AwardProbabilityRange matchedRange = probabilityRanges.get(left);
        int lowerBoundInclusive = left == 0 ? 0 : probabilityRanges.get(left - 1).upperBoundExclusive();
        if (randomIndex < lowerBoundInclusive || randomIndex >= matchedRange.upperBoundExclusive()) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_ASSEMBLY_FAILED, assembleKey, randomIndex);
        }

        return matchedRange.awardId();
    }
}
