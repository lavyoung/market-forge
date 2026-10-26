package com.lavyoung.marketforge.application.strategy.service;

import com.lavyoung.marketforge.application.strategy.model.StrategyReadinessResult;
import com.lavyoung.marketforge.application.strategy.service.impl.StrategyReadinessApplicationServiceImpl;
import com.lavyoung.marketforge.domain.strategy.model.aggregate.StrategyDefinitionAggregate;
import com.lavyoung.marketforge.domain.strategy.model.vo.*;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyDefinitionRepository;
import com.lavyoung.marketforge.domain.strategy.service.IStrategyDefinitionReadinessChecker;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 验证 {@link StrategyReadinessApplicationServiceImpl} 的聚合查询、领域调用和结果转换行为。
 *
 * <p>领域检查规则由 Domain 测试负责，本测试关注 Application 是否保持分层边界、
 * 完整转换检查报告，以及是否正确区分策略不存在和下游系统故障。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class StrategyReadinessApplicationServiceImplTest {

    private static final Long STRATEGY_ID = 100001L;

    @Mock
    private IStrategyDefinitionRepository strategyDefinitionRepository;

    @Mock
    private IStrategyDefinitionReadinessChecker readinessChecker;

    private StrategyReadinessApplicationServiceImpl applicationService;

    /**
     * Given 已隔离的领域仓储和检查器，When 初始化应用服务，Then 每个用例使用独立实例。
     */
    @BeforeEach
    void setUp() {
        applicationService = new StrategyReadinessApplicationServiceImpl(strategyDefinitionRepository, readinessChecker);
    }

    /**
     * Given 存在的策略和完整领域检查报告，When 执行检查，Then 所有问题与概率区间均转换为 Application 结果。
     */
    @Test
    void shouldMapCompleteDomainReportToApplicationResult() {
        // Given
        StrategyDefinitionAggregate aggregate = aggregate();
        StrategyReadinessReportVO report = new StrategyReadinessReportVO(
                STRATEGY_ID,
                List.of(new StrategyReadinessIssueVO("strategy.rate.invalid", "awards[*].awardRate", "概率配置无效")),
                List.of(new StrategyReadinessIssueVO("strategy.stock.warning", "awards[0].awardCountSurplus", "库存偏低")),
                List.of(new StrategyReadinessIssueVO("strategy.subtitle.missing", "awards[0].awardSubtitle", "建议补充副标题")),
                new StrategyProbabilityPreviewVO(true, 10, List.of(
                        new StrategyProbabilityIntervalVO(200001L, 3, 0, 3, new BigDecimal("0.300000000000")),
                        new StrategyProbabilityIntervalVO(200002L, 7, 3, 10, new BigDecimal("0.700000000000"))
                ))
        );
        when(strategyDefinitionRepository.queryByStrategyId(STRATEGY_ID)).thenReturn(Optional.of(aggregate));
        when(readinessChecker.check(aggregate)).thenReturn(report);

        // When
        StrategyReadinessResult result = applicationService.check(STRATEGY_ID);

        // Then
        assertEquals(STRATEGY_ID, result.strategyId());
        assertFalse(result.ready());
        assertEquals("strategy.rate.invalid", result.errors().get(0).code());
        assertEquals("strategy.stock.warning", result.warnings().get(0).code());
        assertEquals("strategy.subtitle.missing", result.suggestions().get(0).code());
        assertEquals(10, result.probabilityPreview().totalWeight());
        assertEquals(List.of(200001L, 200002L), result.probabilityPreview().intervals().stream()
                .map(interval -> interval.awardId())
                .toList());
        verify(strategyDefinitionRepository).queryByStrategyId(STRATEGY_ID);
        verify(readinessChecker).check(aggregate);
    }

    /**
     * Given 策略标识不存在，When 执行检查，Then 返回统一策略不存在业务异常且不调用领域检查器。
     */
    @Test
    void shouldRejectMissingStrategyBeforeDomainCheck() {
        // Given
        when(strategyDefinitionRepository.queryByStrategyId(STRATEGY_ID)).thenReturn(Optional.empty());

        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> applicationService.check(STRATEGY_ID));

        // Then
        assertSame(BusinessResponseCode.STRATEGY_NOT_FOUND, exception.getResponseCode());
        verifyNoInteractions(readinessChecker);
    }

    /**
     * Given 空策略标识，When 执行检查，Then 在访问仓储前拒绝请求。
     */
    @Test
    void shouldRejectNullStrategyIdBeforeRepositoryAccess() {
        // When & Then
        assertThrows(NullPointerException.class, () -> applicationService.check(null));
        verifyNoInteractions(strategyDefinitionRepository, readinessChecker);
    }

    /**
     * Given 领域检查器发生系统故障，When 执行检查，Then 原样向上抛出而不转换成配置结果。
     */
    @Test
    void shouldPropagateReadinessCheckerFailure() {
        // Given
        StrategyDefinitionAggregate aggregate = aggregate();
        IllegalStateException failure = new IllegalStateException("award repository unavailable");
        when(strategyDefinitionRepository.queryByStrategyId(STRATEGY_ID)).thenReturn(Optional.of(aggregate));
        when(readinessChecker.check(aggregate)).thenThrow(failure);

        // When
        IllegalStateException actual = assertThrows(IllegalStateException.class,
                () -> applicationService.check(STRATEGY_ID));

        // Then
        assertSame(failure, actual);
    }

    /**
     * 创建应用服务测试使用的最小策略定义聚合。
     *
     * @return 不包含子配置的草稿策略聚合
     */
    private StrategyDefinitionAggregate aggregate() {
        return new StrategyDefinitionAggregate(STRATEGY_ID, "就绪检查策略", null,
                StrategyDefinitionStateVO.DRAFT, 1, "operator-001", "operator-001", null, null, List.of(), List.of());
    }
}
