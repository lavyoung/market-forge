package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.impl;

import com.lavyoung.marketforge.domain.strategy.repository.IStrategyRepository;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.IProbabilityAlgorithm;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilityAlgorithmType;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityWeight;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.StrategyProbabilitySpace;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.*;

/**
 * O(1) 直接查表概率算法。
 * <p>
 * 装配时按照整数权重重复填充奖品标识，并将乱序后的下标到奖品标识映射保存到
 * Redis Hash。抽奖时生成一个合法随机下标，通过一次 Hash 查询获得奖品标识。
 * <p>
 * 该算法查询速度稳定，但空间复杂度为 O(totalWeight)，因此通过配置限制允许
 * 展开的最大查找表大小，防止极高概率精度耗尽 JVM 或 Redis 内存。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
@Component
public class DirectLookupProbabilityAlgorithm implements IProbabilityAlgorithm {

    private final IStrategyRepository repository;
    private final int maximumTableSize;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 创建直接查表算法。
     *
     * @param repository       策略仓储端口
     * @param maximumTableSize 允许展开的最大查找表元素数量
     * @throws NullPointerException     策略仓储为空时抛出
     * @throws IllegalArgumentException 最大查找表容量小于等于零时抛出
     */
    public DirectLookupProbabilityAlgorithm(
            IStrategyRepository repository,
            @Value("${market-forge.strategy.algorithm.direct-lookup-max-size:10000}") int maximumTableSize) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        if (maximumTableSize <= 0) {
            throw new IllegalArgumentException("maximumTableSize must be greater than zero");
        }
        this.maximumTableSize = maximumTableSize;
    }

    @Override
    public ProbabilityAlgorithmType type() {
        return ProbabilityAlgorithmType.DIRECT_LOOKUP;
    }

    @Override
    public boolean supports(StrategyProbabilitySpace probabilitySpace) {
        Objects.requireNonNull(probabilitySpace, "probabilitySpace must not be null");
        return probabilitySpace.totalWeight() <= maximumTableSize;
    }

    @Override
    public void assemble(StrategyProbabilitySpace probabilitySpace) {
        Objects.requireNonNull(probabilitySpace, "probabilitySpace must not be null");
        if (!supports(probabilitySpace)) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_RATE_TOTAL_INVALID,
                    probabilitySpace.assembleKey(),
                    probabilitySpace.totalWeight(),
                    maximumTableSize);
        }

        List<Long> lookupTable = createLookupTable(probabilitySpace);
        // 避免 Redis 中连续区段直接暴露奖品概率分布 不会对概率产生影响
        Collections.shuffle(lookupTable, secureRandom);

        Map<Integer, Long> indexedLookupTable = new HashMap<>(hashCapacity(lookupTable.size()));
        for (int index = 0; index < lookupTable.size(); index++) {
            indexedLookupTable.put(index, lookupTable.get(index));
        }

        // 缓存查找表
        repository.storeStrategyAwardSearchTables(probabilitySpace.assembleKey(), probabilitySpace.totalWeight(), indexedLookupTable);
    }

    @Override
    public long dispatch(String assembleKey) {
        if (StringUtils.isBlank(assembleKey)) {
            throw new IllegalArgumentException("assembleKey must not be blank");
        }

        int rateRange = repository.getRateRange(assembleKey);
        if (rateRange <= 0) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_NOT_ASSEMBLED);
        }

        int randomIndex = secureRandom.nextInt(rateRange);
        long awardId = repository.getStrategyAwardAssemble(assembleKey, randomIndex);
        if (awardId <= 0) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_ASSEMBLY_FAILED, assembleKey, randomIndex);
        }
        return awardId;
    }

    /**
     * 按整数权重展开奖品标识查找表。
     *
     * @param probabilitySpace 完整概率空间
     * @return 元素数量等于总权重的奖品标识列表
     */
    private List<Long> createLookupTable(StrategyProbabilitySpace probabilitySpace) {
        List<Long> lookupTable = new ArrayList<>(probabilitySpace.totalWeight());

        for (AwardProbabilityWeight awardWeight : probabilitySpace.awardWeights()) {
            for (int i = 0; i < awardWeight.weight(); i++) {
                lookupTable.add(awardWeight.awardId());
            }
        }

        return lookupTable;
    }

    /**
     * 根据元素数量计算 HashMap 初始容量，避免装配过程中扩容。
     *
     * @param elementCount 查找表元素数量
     * @return 满足默认负载因子的 HashMap 初始容量
     */
    private int hashCapacity(int elementCount) {
        return (int) Math.ceil(elementCount / 0.75D) + 1;
    }
}
