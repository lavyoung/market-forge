package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm;

import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityWeight;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.StrategyProbabilitySpace;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 策略概率空间工厂。
 * <p>
 * 负责校验策略奖品概率，并将数据库中的十进制概率精确转换成最小等价整数权重。
 * 工厂只完成概率模型转换，不选择具体抽奖算法，也不写入 Redis。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/19
 */
@Component
public class ProbabilitySpaceFactory {

    private static final BigInteger MAX_TOTAL_WEIGHT = BigInteger.valueOf(Integer.MAX_VALUE);

    /**
     * 根据策略奖品配置建立完整的整数概率空间。
     * <p>
     * 所有概率先按照最大精度转换为整数，再统一除以最大公约数，以减少直接查表算法
     * 所需的存储空间。转换过程使用 {@link BigInteger} 计算，避免中间结果发生溢出。
     *
     * @param assembleKey    策略装配键，可以是策略标识或“策略标识_权重档位”
     * @param strategyAwards 参与本次装配的策略奖品
     * @return 已完成校验和整数化的概率空间
     * @throws BusinessException 奖品缺失、概率非法、奖品重复或约简后的概率空间超过整型范围时抛出
     */
    public StrategyProbabilitySpace create(String assembleKey, List<StrategyAwardEntity> strategyAwards) {
        validateStrategyAwards(assembleKey, strategyAwards);

        int maximumScale = strategyAwards.stream()
                .map(StrategyAwardEntity::awardRate)
                .map(BigDecimal::stripTrailingZeros)
                .mapToInt(rate -> Math.max(rate.scale(), 0))
                .max()
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.STRATEGY_AWARD_NOT_CONFIGURED, assembleKey));

        List<ScaledAwardWeight> scaledAwardWeights = strategyAwards.stream()
                .map(award -> toScaledWeight(award, maximumScale))
                .toList();

        // 最大公约数
        BigInteger greatestCommonDivisor = scaledAwardWeights.stream()
                .map(ScaledAwardWeight::weight)
                .reduce(BigInteger.ZERO, BigInteger::gcd);

        List<AwardProbabilityWeight> awardWeights = new ArrayList<>(scaledAwardWeights.size());
        BigInteger totalWeight = BigInteger.ZERO;

        for (ScaledAwardWeight scaledAwardWeight : scaledAwardWeights) {
            // 权重
            BigInteger reducedWeight = scaledAwardWeight.weight().divide(greatestCommonDivisor);
            ensureIntegerRange(assembleKey, scaledAwardWeight.awardId(), reducedWeight);

            awardWeights.add(new AwardProbabilityWeight(
                    scaledAwardWeight.awardId(),
                    reducedWeight.intValueExact()
            ));
            totalWeight = totalWeight.add(reducedWeight);
        }

        if (totalWeight.compareTo(MAX_TOTAL_WEIGHT) > 0) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_RATE_TOTAL_INVALID, assembleKey, totalWeight);
        }

        return new StrategyProbabilitySpace(assembleKey, totalWeight.intValueExact(), awardWeights);
    }


    /**
     * 校验策略奖品集合及每个奖品的基础概率配置。
     *
     * @param assembleKey    策略装配键
     * @param strategyAwards 策略奖品配置
     * @throws BusinessException 装配键、奖品标识、概率或奖品唯一性不合法时抛出
     */
    private void validateStrategyAwards(String assembleKey, List<StrategyAwardEntity> strategyAwards) {
        if (assembleKey == null || assembleKey.isBlank()) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_RATE_INVALID, assembleKey);
        }
        if (CollectionUtils.isEmpty(strategyAwards)) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_AWARD_NOT_CONFIGURED, assembleKey);
        }
        Set<Long> awardIds = new HashSet<>();
        for (StrategyAwardEntity strategyAward : strategyAwards) {
            if (strategyAward == null || strategyAward.awardId() == null || strategyAward.awardId() <= 0) {
                throw BusinessException.of(BusinessResponseCode.STRATEGY_RATE_INVALID, assembleKey, "awardId");
            }

            BigDecimal awardRate = strategyAward.awardRate();
            if (awardRate == null || awardRate.compareTo(BigDecimal.ZERO) <= 0) {
                throw BusinessException.of(BusinessResponseCode.STRATEGY_RATE_INVALID, assembleKey, strategyAward.awardId(), awardRate);
            }

            if (!awardIds.add(strategyAward.awardId())) {
                throw BusinessException.of(BusinessResponseCode.STRATEGY_RATE_INVALID, assembleKey, strategyAward.awardId(), "duplicated award");
            }
        }
    }

    /**
     * 按统一小数位数将奖品概率无损转换为整数权重。
     *
     * @param strategyAward 策略奖品配置
     * @param maximumScale  本次概率集合的最大小数位数
     * @return 尚未约分的奖品整数权重
     * @throws BusinessException 概率无法精确转换为整数时抛出
     */
    private ScaledAwardWeight toScaledWeight(StrategyAwardEntity strategyAward, int maximumScale) {
        try {
            BigInteger weight = strategyAward.awardRate()
                    .stripTrailingZeros()
                    .movePointRight(maximumScale)
                    .toBigIntegerExact();

            return new ScaledAwardWeight(strategyAward.awardId(), weight);
        } catch (ArithmeticException exception) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_RATE_INVALID, exception,
                    strategyAward.strategyId(), strategyAward.awardId(), strategyAward.awardRate());
        }
    }

    /**
     * 校验约分后的单个奖品权重是否能够使用 int 表示。
     *
     * @param assembleKey 策略装配键
     * @param awardId     奖品标识
     * @param weight      约分后的整数权重
     * @throws BusinessException 权重不是正数或超过整型最大值时抛出
     */
    private void ensureIntegerRange(String assembleKey, long awardId, BigInteger weight) {
        if (weight.signum() <= 0 || weight.compareTo(MAX_TOTAL_WEIGHT) > 0) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_RATE_INVALID, assembleKey, awardId, weight);
        }
    }

    /**
     * 尚未使用最大公约数约分的奖品权重。
     *
     * @param awardId 奖品标识
     * @param weight  按最大小数位放大后的精确整数权重
     */
    private record ScaledAwardWeight(long awardId, BigInteger weight) {
    }

}
