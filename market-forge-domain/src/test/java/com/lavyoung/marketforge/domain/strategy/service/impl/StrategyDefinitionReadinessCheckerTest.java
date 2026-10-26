package com.lavyoung.marketforge.domain.strategy.service.impl;

import com.lavyoung.marketforge.domain.strategy.model.aggregate.StrategyDefinitionAggregate;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyDefinitionStateVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyProbabilityIntervalVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyProbabilityPreviewVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyReadinessReportVO;
import com.lavyoung.marketforge.domain.strategy.service.IStrategyDefinitionPublicationValidator;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.StrategyProbabilityPreviewFactory;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证 {@link StrategyDefinitionReadinessChecker} 对发布校验、概率预览和非阻断提示的编排行为。
 *
 * <p>发布规则正确性由发布校验器自身测试负责；本测试重点保证业务异常被转换成
 * 可展示报告，而未知系统异常不会被误判为运营配置错误。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class StrategyDefinitionReadinessCheckerTest {

    private static final Long STRATEGY_ID = 100001L;

    @Mock
    private IStrategyDefinitionPublicationValidator validator;

    @Mock
    private StrategyProbabilityPreviewFactory previewFactory;

    private StrategyDefinitionReadinessChecker readinessChecker;

    /**
     * Given 已隔离的发布校验器和概率预览工厂，When 初始化检查器，Then 每个用例使用独立实例。
     */
    @BeforeEach
    void setUp() {
        readinessChecker = new StrategyDefinitionReadinessChecker(validator, previewFactory);
    }

    /**
     * Given 满足发布条件的策略，When 执行就绪检查，Then 返回可发布报告和正式概率预览。
     */
    @Test
    void shouldReturnReadyReportWithProbabilityPreview() {
        // Given
        StrategyDefinitionAggregate aggregate = aggregate(List.of(
                award(200001L, "一等奖", "稀有奖品", 30, 30, "0.30", 1),
                award(200002L, "二等奖", "常规奖品", 70, 70, "0.70", 2)
        ));
        StrategyProbabilityPreviewVO preview = new StrategyProbabilityPreviewVO(true, 10, List.of(
                new StrategyProbabilityIntervalVO(200001L, 3, 0, 3, new BigDecimal("0.300000000000")),
                new StrategyProbabilityIntervalVO(200002L, 7, 3, 10, new BigDecimal("0.700000000000"))
        ));
        when(previewFactory.create(String.valueOf(STRATEGY_ID), aggregate.awards())).thenReturn(preview);

        // When
        StrategyReadinessReportVO report = readinessChecker.check(aggregate);

        // Then
        assertTrue(report.ready());
        assertTrue(report.errors().isEmpty());
        assertTrue(report.warnings().isEmpty());
        assertTrue(report.suggestions().isEmpty());
        assertSame(preview, report.probabilityPreview());
        verify(validator).validate(aggregate);
        verify(previewFactory).create(String.valueOf(STRATEGY_ID), aggregate.awards());
    }

    /**
     * Given 已消耗库存且缺少副标题的单奖品策略，When 执行检查，Then 返回警告和两项优化建议。
     */
    @Test
    void shouldCollectWarningsAndSuggestionsWithoutBlockingPublication() {
        // Given
        StrategyDefinitionAggregate aggregate = aggregate(List.of(
                award(200001L, "单一奖品", null, 100, 80, "1.00", 1)
        ));
        StrategyProbabilityPreviewVO preview = new StrategyProbabilityPreviewVO(true, 1, List.of(
                new StrategyProbabilityIntervalVO(200001L, 1, 0, 1, new BigDecimal("1.000000000000"))
        ));
        when(previewFactory.create(String.valueOf(STRATEGY_ID), aggregate.awards())).thenReturn(preview);

        // When
        StrategyReadinessReportVO report = readinessChecker.check(aggregate);

        // Then
        assertTrue(report.ready());
        assertEquals(List.of("strategy.award.stock-already-consumed"), report.warnings().stream()
                .map(issue -> issue.code())
                .toList());
        assertEquals(List.of("strategy.award.subtitle-missing", "strategy.single-award"), report.suggestions().stream()
                .map(issue -> issue.code())
                .toList());
    }

    /**
     * Given 发布校验发现概率非法，When 执行检查，Then 转换成概率字段错误且不生成预览。
     */
    @Test
    void shouldConvertPublicationBusinessExceptionToReadinessError() {
        // Given
        StrategyDefinitionAggregate aggregate = aggregate(List.of(
                award(200001L, "测试奖品", "概率异常", 100, 100, "1.00", 1)
        ));
        doThrow(BusinessException.of(BusinessResponseCode.STRATEGY_RATE_INVALID, STRATEGY_ID))
                .when(validator).validate(aggregate);

        // When
        StrategyReadinessReportVO report = readinessChecker.check(aggregate);

        // Then
        assertFalse(report.ready());
        assertEquals(1, report.errors().size());
        assertEquals("strategy.rate.invalid", report.errors().get(0).code());
        assertEquals("awards[*].awardRate", report.errors().get(0).path());
        assertFalse(report.probabilityPreview().available());
        verifyNoInteractions(previewFactory);
    }

    /**
     * Given 发布校验成功但概率预览暴露配置业务异常，When 执行检查，Then 返回不可用预览和阻断错误。
     */
    @Test
    void shouldConvertProbabilityPreviewBusinessExceptionToReadinessError() {
        // Given
        StrategyDefinitionAggregate aggregate = aggregate(List.of(
                award(200001L, "测试奖品", "概率异常", 100, 100, "1.00", 1)
        ));
        when(previewFactory.create(String.valueOf(STRATEGY_ID), aggregate.awards()))
                .thenThrow(BusinessException.of(BusinessResponseCode.STRATEGY_RATE_TOTAL_INVALID, STRATEGY_ID));

        // When
        StrategyReadinessReportVO report = readinessChecker.check(aggregate);

        // Then
        assertFalse(report.ready());
        assertEquals("strategy.rate.total-invalid", report.errors().get(0).code());
        assertEquals("awards[*].awardRate", report.errors().get(0).path());
        assertFalse(report.probabilityPreview().available());
    }

    /**
     * Given 发布校验依赖发生未知系统异常，When 执行检查，Then 原样向上抛出而不是伪装成配置错误。
     */
    @Test
    void shouldPropagateInfrastructureFailure() {
        // Given
        StrategyDefinitionAggregate aggregate = aggregate(List.of(
                award(200001L, "测试奖品", "正常奖品", 100, 100, "1.00", 1)
        ));
        IllegalStateException failure = new IllegalStateException("award repository unavailable");
        doThrow(failure).when(validator).validate(aggregate);

        // When
        IllegalStateException actual = assertThrows(IllegalStateException.class,
                () -> readinessChecker.check(aggregate));

        // Then
        assertSame(failure, actual);
        verifyNoInteractions(previewFactory);
    }

    /**
     * Given 空策略聚合，When 执行检查，Then 在调用任何依赖前拒绝请求。
     */
    @Test
    void shouldRejectNullAggregateBeforeCallingDependencies() {
        // When & Then
        assertThrows(NullPointerException.class, () -> readinessChecker.check(null));
        verifyNoInteractions(validator, previewFactory);
    }

    /**
     * 创建就绪检查测试使用的最小策略定义聚合。
     *
     * @param awards 策略奖品集合
     * @return 草稿状态的策略定义聚合
     */
    private StrategyDefinitionAggregate aggregate(List<StrategyAwardEntity> awards) {
        return new StrategyDefinitionAggregate(STRATEGY_ID, "就绪检查策略", null,
                StrategyDefinitionStateVO.DRAFT, 1, "operator-001", "operator-001", null, null, awards, List.of());
    }

    /**
     * 创建包含库存、概率和展示信息的策略奖品。
     *
     * @param awardId           奖品标识
     * @param awardTitle        奖品标题
     * @param awardSubtitle     奖品副标题
     * @param awardCount        奖品库存总量
     * @param awardCountSurplus 奖品剩余库存
     * @param awardRate         奖品概率文本
     * @param sort              奖品展示顺序
     * @return 策略奖品实体
     */
    private StrategyAwardEntity award(Long awardId, String awardTitle, String awardSubtitle, int awardCount,
                                      int awardCountSurplus, String awardRate, int sort) {
        return new StrategyAwardEntity(STRATEGY_ID, awardId, awardTitle, awardSubtitle, awardCount,
                awardCountSurplus, new BigDecimal(awardRate), null, sort, null, null);
    }
}
