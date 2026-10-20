package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm;

import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.StrategyProbabilitySpace;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 概率抽奖算法注册表。
 *
 * <p>负责收集 Spring 容器中的全部概率算法，根据枚举声明顺序选择第一个
 * 支持当前概率空间的算法，并支持抽奖阶段按已保存的算法类型恢复实现。</p>
 *
 * <p>{@link ProbabilityAlgorithmType} 的声明顺序同时代表算法选择优先级。
 * 当前先尝试空间换时间的直接查表算法；直接查表容量不足时，大型候选奖池
 * 使用 Alias Method；候选数量较少但总权重较大的概率空间使用二分算法兜底。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
@Component
public class ProbabilityAlgorithmRegistry {

    private final Map<ProbabilityAlgorithmType, IProbabilityAlgorithm> algorithms;

    /**
     * 收集并校验概率算法实现。
     *
     * @param algorithmList Spring 容器中的概率算法实现
     * @throws NullPointerException  算法集合或其中的算法实现为空时抛出
     * @throws IllegalStateException 同一算法类型存在多个实现时抛出
     */
    public ProbabilityAlgorithmRegistry(List<IProbabilityAlgorithm> algorithmList) {
        Objects.requireNonNull(algorithmList, "algorithmList must not be null");

        EnumMap<ProbabilityAlgorithmType, IProbabilityAlgorithm> registeredAlgorithms = new EnumMap<>(ProbabilityAlgorithmType.class);

        for (IProbabilityAlgorithm algorithm : algorithmList) {
            Objects.requireNonNull(algorithm, "algorithm must not be null");
            IProbabilityAlgorithm previous = registeredAlgorithms.putIfAbsent(algorithm.type(), algorithm);

            if (previous != null) {
                throw new IllegalStateException("duplicate probability algorithm type: " + algorithm.type());
            }
        }

        this.algorithms = Map.copyOf(registeredAlgorithms);
    }

    /**
     * 为概率空间选择第一个可安全装配的算法。
     *
     * <p>算法按照 {@link ProbabilityAlgorithmType} 的声明顺序判断。直接查表
     * 优先处理可安全展开的小概率空间；Alias Method 处理中大型候选奖池；
     * 二分算法作为不展开总权重的通用兜底实现。</p>
     *
     * @param probabilitySpace 已完成校验和整数化的概率空间
     * @return 首个支持该概率空间的算法实现
     * @throws NullPointerException 概率空间为空时抛出
     * @throws BusinessException    没有算法能够处理该概率空间时抛出
     */
    public IProbabilityAlgorithm select(StrategyProbabilitySpace probabilitySpace) {
        Objects.requireNonNull(probabilitySpace, "probabilitySpace must not be null");
        for (ProbabilityAlgorithmType algorithmType : ProbabilityAlgorithmType.values()) {
            IProbabilityAlgorithm algorithm = algorithms.get(algorithmType);
            if (algorithm != null && algorithm.supports(probabilitySpace)) {
                return algorithm;
            }
        }
        throw BusinessException.of(BusinessResponseCode.STRATEGY_RATE_TOTAL_INVALID, probabilitySpace.assembleKey(), probabilitySpace.totalWeight());
    }


    /**
     * 根据装配元数据获取指定算法实现。
     *
     * @param algorithmType 装配阶段保存的算法类型
     * @return 与算法类型对应的实现
     * @throws NullPointerException 算法类型为空时抛出
     * @throws BusinessException    当前应用没有注册对应算法实现时抛出
     */
    public IProbabilityAlgorithm getRequired(ProbabilityAlgorithmType algorithmType) {
        Objects.requireNonNull(algorithmType, "algorithmType must not be null");
        IProbabilityAlgorithm algorithm = algorithms.get(algorithmType);
        if (algorithm == null) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_ASSEMBLY_FAILED, algorithmType.getCode());
        }

        return algorithm;
    }
}
