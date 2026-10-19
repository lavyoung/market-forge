package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm;

import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.AwardProbabilityWeight;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.model.StrategyProbabilitySpace;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 {@link ProbabilitySpaceFactory} 的概率校验、精确整数化和约分行为。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class ProbabilitySpaceFactoryTest {

    private static final String ASSEMBLE_KEY = "900901001";
    private static final Long STRATEGY_ID = 900901001L;

    private ProbabilitySpaceFactory probabilitySpaceFactory;

    /**
     * Given 无外部依赖的概率空间工厂，When 初始化测试对象，Then 每个用例使用独立实例。
     */
    @BeforeEach
    void setUp() {
        probabilitySpaceFactory = new ProbabilitySpaceFactory();
    }

    /**
     * Given 九宫格真实奖品概率，When 创建概率空间，Then 精确约分为总权重 200。
     */
    @Test
    void shouldReduceNineGridAwardRatesWithoutChangingRatio() {
        // Given
        List<StrategyAwardEntity> strategyAwards = List.of(
                award(900901011L, "0.30000"),
                award(900901012L, "0.20000"),
                award(900901013L, "0.15000"),
                award(900901014L, "0.12000"),
                award(900901015L, "0.10000"),
                award(900901016L, "0.08000"),
                award(900901017L, "0.01000"),
                award(900901018L, "0.01500"),
                award(900901019L, "0.02500")
        );

        // When
        StrategyProbabilitySpace probabilitySpace = probabilitySpaceFactory.create(ASSEMBLE_KEY, strategyAwards);

        // Then
        assertEquals(200, probabilitySpace.totalWeight());
        assertEquals(List.of(
                new AwardProbabilityWeight(900901011L, 60),
                new AwardProbabilityWeight(900901012L, 40),
                new AwardProbabilityWeight(900901013L, 30),
                new AwardProbabilityWeight(900901014L, 24),
                new AwardProbabilityWeight(900901015L, 20),
                new AwardProbabilityWeight(900901016L, 16),
                new AwardProbabilityWeight(900901017L, 2),
                new AwardProbabilityWeight(900901018L, 3),
                new AwardProbabilityWeight(900901019L, 5)
        ), probabilitySpace.awardWeights());
    }

    /**
     * Given 不同小数精度的奖品概率，When 创建概率空间，Then 保留最小概率对应的精确整数单位。
     */
    @Test
    void shouldPreserveDifferentDecimalScalesExactly() {
        // Given
        List<StrategyAwardEntity> strategyAwards = List.of(
                award(101L, "0.79"),
                award(102L, "0.03"),
                award(103L, "0.0003")
        );

        // When
        StrategyProbabilitySpace probabilitySpace = probabilitySpaceFactory.create(ASSEMBLE_KEY, strategyAwards);

        // Then
        assertEquals(8203, probabilitySpace.totalWeight());
        assertEquals(List.of(
                new AwardProbabilityWeight(101L, 7900),
                new AwardProbabilityWeight(102L, 300),
                new AwardProbabilityWeight(103L, 3)
        ), probabilitySpace.awardWeights());
    }

    /**
     * Given 一个权重子奖池，When 单独创建概率空间，Then 使用子奖池内部最大公约数约分。
     */
    @Test
    void shouldNormalizeWeightedAwardPoolIndependently() {
        // Given
        List<StrategyAwardEntity> strategyAwards = List.of(
                award(900901017L, "0.010"),
                award(900901018L, "0.015"),
                award(900901019L, "0.025")
        );

        // When
        StrategyProbabilitySpace probabilitySpace = probabilitySpaceFactory.create("900901001_60000", strategyAwards);

        // Then
        assertEquals(10, probabilitySpace.totalWeight());
        assertEquals(List.of(
                new AwardProbabilityWeight(900901017L, 2),
                new AwardProbabilityWeight(900901018L, 3),
                new AwardProbabilityWeight(900901019L, 5)
        ), probabilitySpace.awardWeights());
    }

    /**
     * Given 空白装配键或没有奖品配置，When 创建概率空间，Then 返回对应业务错误。
     */
    @Test
    void shouldRejectMissingAssemblyInput() {
        // Given & When
        BusinessException blankKeyException = assertThrows(BusinessException.class,
                () -> probabilitySpaceFactory.create(" ", List.of(award(101L, "1.0"))));
        BusinessException emptyAwardsException = assertThrows(BusinessException.class,
                () -> probabilitySpaceFactory.create(ASSEMBLE_KEY, List.of()));

        // Then
        assertSame(BusinessResponseCode.STRATEGY_RATE_INVALID, blankKeyException.getResponseCode());
        assertSame(BusinessResponseCode.STRATEGY_AWARD_NOT_CONFIGURED, emptyAwardsException.getResponseCode());
    }

    /**
     * Given 零概率、负概率或重复奖品，When 创建概率空间，Then 拒绝不确定的抽奖配置。
     */
    @Test
    void shouldRejectInvalidOrDuplicatedAwardRates() {
        // Given & When
        BusinessException zeroRateException = assertThrows(BusinessException.class,
                () -> probabilitySpaceFactory.create(ASSEMBLE_KEY, List.of(award(101L, "0"))));
        BusinessException negativeRateException = assertThrows(BusinessException.class,
                () -> probabilitySpaceFactory.create(ASSEMBLE_KEY, List.of(award(101L, "-0.1"))));
        BusinessException duplicatedAwardException = assertThrows(BusinessException.class,
                () -> probabilitySpaceFactory.create(ASSEMBLE_KEY, List.of(award(101L, "0.6"), award(101L, "0.4"))));

        // Then
        assertSame(BusinessResponseCode.STRATEGY_RATE_INVALID, zeroRateException.getResponseCode());
        assertSame(BusinessResponseCode.STRATEGY_RATE_INVALID, negativeRateException.getResponseCode());
        assertSame(BusinessResponseCode.STRATEGY_RATE_INVALID, duplicatedAwardException.getResponseCode());
    }

    /**
     * Given 各奖品权重均未越界但权重总和超过整型上限，When 创建概率空间，Then 暴露总概率范围错误。
     */
    @Test
    void shouldRejectTotalWeightOverflow() {
        // Given
        List<StrategyAwardEntity> strategyAwards = List.of(
                award(101L, "1.000000000"),
                award(102L, "0.999999999"),
                award(103L, "0.999999998")
        );

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> probabilitySpaceFactory.create(ASSEMBLE_KEY, strategyAwards));

        // Then
        assertSame(BusinessResponseCode.STRATEGY_RATE_TOTAL_INVALID, exception.getResponseCode());
    }

    /**
     * 构造仅包含概率转换所需字段的策略奖品。
     *
     * @param awardId   奖品标识
     * @param awardRate 奖品概率文本
     * @return 策略奖品实体
     */
    private StrategyAwardEntity award(Long awardId, String awardRate) {
        return StrategyAwardEntity.builder()
                .strategyId(STRATEGY_ID)
                .awardId(awardId)
                .awardRate(new BigDecimal(awardRate))
                .build();
    }
}
