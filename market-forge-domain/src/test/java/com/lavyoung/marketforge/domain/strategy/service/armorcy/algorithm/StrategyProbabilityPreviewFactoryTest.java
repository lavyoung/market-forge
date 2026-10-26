package com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm;

import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyProbabilityIntervalVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyProbabilityPreviewVO;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 {@link StrategyProbabilityPreviewFactory} 与正式整数概率空间保持一致。
 *
 * <p>测试重点覆盖连续区间、奖品顺序、概率精度和非法配置异常透传，防止后台
 * 预览结果与正式抽奖装配结果产生偏差。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
class StrategyProbabilityPreviewFactoryTest {

    private static final String ASSEMBLE_KEY = "900901001";
    private static final Long STRATEGY_ID = 900901001L;

    private StrategyProbabilityPreviewFactory previewFactory;

    /**
     * Given 正式概率空间工厂，When 初始化预览工厂，Then 每个用例使用独立实例。
     */
    @BeforeEach
    void setUp() {
        previewFactory = new StrategyProbabilityPreviewFactory(new ProbabilitySpaceFactory());
    }

    /**
     * Given 三个概率不同的奖品，When 创建概率预览，Then 区间应从零开始连续覆盖全部整数权重。
     */
    @Test
    void shouldCreateContinuousProbabilityIntervals() {
        // Given
        List<StrategyAwardEntity> strategyAwards = List.of(
                award(101L, "0.15"),
                award(102L, "0.35"),
                award(103L, "0.50")
        );

        // When
        StrategyProbabilityPreviewVO preview = previewFactory.create(ASSEMBLE_KEY, strategyAwards);

        // Then
        assertTrue(preview.available());
        assertEquals(20, preview.totalWeight());
        assertEquals(List.of(
                new StrategyProbabilityIntervalVO(101L, 3, 0, 3, new BigDecimal("0.150000000000")),
                new StrategyProbabilityIntervalVO(102L, 7, 3, 10, new BigDecimal("0.350000000000")),
                new StrategyProbabilityIntervalVO(103L, 10, 10, 20, new BigDecimal("0.500000000000"))
        ), preview.intervals());
    }

    /**
     * Given 非按奖品标识排序的输入，When 创建概率预览，Then 应保留正式概率空间的输入顺序。
     */
    @Test
    void shouldPreserveProbabilitySpaceAwardOrder() {
        // Given
        List<StrategyAwardEntity> strategyAwards = List.of(
                award(303L, "0.20"),
                award(101L, "0.30"),
                award(202L, "0.50")
        );

        // When
        StrategyProbabilityPreviewVO preview = previewFactory.create(ASSEMBLE_KEY, strategyAwards);

        // Then
        assertEquals(List.of(303L, 101L, 202L), preview.intervals().stream()
                .map(StrategyProbabilityIntervalVO::awardId)
                .toList());
    }

    /**
     * Given 总权重接近整型上限且最小权重为一，When 创建概率预览，Then 最小理论概率不应舍入为零。
     */
    @Test
    void shouldKeepSmallestProbabilityGreaterThanZero() {
        // Given
        List<StrategyAwardEntity> strategyAwards = List.of(
                award(101L, "0.000000001"),
                award(102L, "2.000000000")
        );

        // When
        StrategyProbabilityPreviewVO preview = previewFactory.create(ASSEMBLE_KEY, strategyAwards);

        // Then
        assertEquals(2_000_000_001, preview.totalWeight());
        assertTrue(preview.intervals().get(0).probability().compareTo(BigDecimal.ZERO) > 0);
    }

    /**
     * Given 重复奖品配置，When 创建概率预览，Then 应透传正式概率空间的业务异常。
     */
    @Test
    void shouldPropagateInvalidProbabilityConfiguration() {
        // Given
        List<StrategyAwardEntity> strategyAwards = List.of(
                award(101L, "0.40"),
                award(101L, "0.60")
        );

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> previewFactory.create(ASSEMBLE_KEY, strategyAwards));

        // Then
        assertSame(BusinessResponseCode.STRATEGY_RATE_INVALID, exception.getResponseCode());
    }

    /**
     * 构造概率预览测试所需的最小策略奖品实体。
     *
     * @param awardId   奖品标识
     * @param awardRate 奖品概率文本
     * @return 包含策略标识、奖品标识和中奖概率的策略奖品实体
     */
    private StrategyAwardEntity award(Long awardId, String awardRate) {
        return StrategyAwardEntity.builder()
                .strategyId(STRATEGY_ID)
                .awardId(awardId)
                .awardRate(new BigDecimal(awardRate))
                .build();
    }
}
