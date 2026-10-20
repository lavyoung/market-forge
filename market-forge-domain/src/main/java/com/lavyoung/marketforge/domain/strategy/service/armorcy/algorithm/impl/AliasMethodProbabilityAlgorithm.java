package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.impl;

import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.AliasProbabilityTableFactory;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.IProbabilityAlgorithm;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilityAlgorithmType;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AliasProbabilityTable;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.StrategyProbabilitySpace;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.random.RandomGenerator;

/**
 * 基于 Alias Method 的常数时间概率抽样算法。
 *
 * <p>装配阶段使用 {@link AliasProbabilityTableFactory} 将原始整数权重转换为
 * 与奖品数量等长的概率表。每一列包含原奖品接受概率，以及未接受时使用的
 * 候补别名列。</p>
 *
 * <p>抽样阶段生成一个位于 {@code [0, n)} 的随机小数，其中整数部分用于
 * 等概率选择一列，小数部分用于判断是否接受当前列。接受时返回当前列奖品，
 * 否则返回别名列奖品。一次随机数生成即可完成抽样，表内查找复杂度为
 * O(1)，概率表空间复杂度为 O(n)。</p>
 *
 * <p>当前第一版从仓储读取完整概率表，因此 O(1) 仅描述表内抽样过程。
 * Redis 反序列化和网络开销将在后续“权重频繁变化”作业中通过带版本的
 * 本地不可变快照缓存优化。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/20
 */
@Component
public class AliasMethodProbabilityAlgorithm implements IProbabilityAlgorithm {

    private final IStrategyRepository repository;
    private final AliasProbabilityTableFactory probabilityTableFactory;
    private final RandomGenerator randomGenerator;
    private final int minimumAwardCount;

    /**
     * 创建生产环境使用的 Alias Method 算法。
     *
     * @param repository              策略仓储端口
     * @param probabilityTableFactory 别名概率表装配器
     * @param minimumAwardCount       启用 Alias Method 所需的最小候选奖品数量
     * @throws NullPointerException     仓储或概率表装配器为空时抛出
     * @throws IllegalArgumentException 最小候选奖品数量小于等于零时抛出
     */
    @Autowired
    public AliasMethodProbabilityAlgorithm(
            IStrategyRepository repository,
            AliasProbabilityTableFactory probabilityTableFactory,
            @Value("${market-forge.strategy.algorithm.alias-min-award-count:1000}") int minimumAwardCount) {
        this(repository, probabilityTableFactory, new RuntimeOptions(new SecureRandom(), minimumAwardCount));
    }

    /**
     * 创建可注入随机数生成器的 Alias Method 算法。
     *
     * <p>该构造器保持包级可见，供同包单元测试注入确定性随机数，不作为
     * 领域模块的公开构造协议。</p>
     *
     * @param repository              策略仓储端口
     * @param probabilityTableFactory 别名概率表装配器
     * @param runtimeOptions          随机数生成器和算法适用阈值
     * @throws NullPointerException     任一依赖为空时抛出
     * @throws IllegalArgumentException 最小候选奖品数量小于等于零时抛出
     */
    AliasMethodProbabilityAlgorithm(
            IStrategyRepository repository,
            AliasProbabilityTableFactory probabilityTableFactory,
            RuntimeOptions runtimeOptions) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.probabilityTableFactory = Objects.requireNonNull(probabilityTableFactory, "probabilityTableFactory must not be null");

        RuntimeOptions options = Objects.requireNonNull(runtimeOptions, "runtimeOptions must not be null");
        this.randomGenerator = options.randomGenerator();
        this.minimumAwardCount = options.minimumAwardCount();
    }

    @Override
    public ProbabilityAlgorithmType type() {
        return ProbabilityAlgorithmType.ALIAS_METHOD;
    }

    @Override
    public boolean supports(StrategyProbabilitySpace probabilitySpace) {
        Objects.requireNonNull(probabilitySpace, "probabilitySpace must not be null");
        return probabilitySpace.awardWeights().size() >= minimumAwardCount;
    }

    @Override
    public void assemble(StrategyProbabilitySpace probabilitySpace) {
        Objects.requireNonNull(probabilitySpace, "probabilitySpace must not be null");

        if (!supports(probabilitySpace)) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_RATE_TOTAL_INVALID,
                    probabilitySpace.assembleKey(),
                    probabilitySpace.awardWeights().size(),
                    minimumAwardCount);
        }

        AliasProbabilityTable aliasProbabilityTable = probabilityTableFactory.create(probabilitySpace);
        repository.storeStrategyAwardAliasTable(probabilitySpace.assembleKey(), aliasProbabilityTable);
    }

    @Override
    public long dispatch(String assembleKey) {
        if (StringUtils.isBlank(assembleKey)) {
            throw new IllegalArgumentException("assembleKey must not be blank");
        }
        AliasProbabilityTable aliasTable = repository.getStrategyAwardAliasTable(assembleKey)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.STRATEGY_NOT_ASSEMBLED, assembleKey));

        /*
         * 一个 [0, n) 随机小数同时承载两部分信息：
         * 整数部分决定候选列，小数部分决定是否接受该列。
         */
        double randomSample = randomGenerator.nextDouble(aliasTable.size());
        int selectedColumn = (int) randomSample;
        double acceptanceValue = randomSample - selectedColumn;
        int awardColumn = acceptanceValue < aliasTable.acceptanceProbabilityAt(selectedColumn)
                ? selectedColumn : aliasTable.aliasIndexAt(selectedColumn);
        return aliasTable.awardIdAt(awardColumn);
    }

    /**
     * Alias Method 运行参数。
     *
     * <p>把随机数生成器和算法选择阈值封装为一个对象，使构造参数保持清晰，
     * 同时允许测试注入可预测的随机序列。</p>
     *
     * @param randomGenerator   生成随机列和接受概率的随机数生成器
     * @param minimumAwardCount 启用 Alias Method 所需的最小候选奖品数量
     */
    record RuntimeOptions(RandomGenerator randomGenerator, int minimumAwardCount) {

        /**
         * 校验运行参数。
         *
         * @throws NullPointerException     随机数生成器为空时抛出
         * @throws IllegalArgumentException 最小候选奖品数量小于等于零时抛出
         */
        RuntimeOptions {
            Objects.requireNonNull(randomGenerator, "randomGenerator must not be null");
            if (minimumAwardCount <= 0) {
                throw new IllegalArgumentException("minimumAwardCount must be greater than zero");
            }
        }
    }
}
