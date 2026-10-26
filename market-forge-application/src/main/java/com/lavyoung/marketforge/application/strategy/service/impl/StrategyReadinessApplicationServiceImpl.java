package com.lavyoung.marketforge.application.strategy.service.impl;

import com.lavyoung.marketforge.application.strategy.model.StrategyProbabilityIntervalResult;
import com.lavyoung.marketforge.application.strategy.model.StrategyProbabilityPreviewResult;
import com.lavyoung.marketforge.application.strategy.model.StrategyReadinessIssueResult;
import com.lavyoung.marketforge.application.strategy.model.StrategyReadinessResult;
import com.lavyoung.marketforge.application.strategy.service.IStrategyReadinessApplicationService;
import com.lavyoung.marketforge.domain.strategy.model.aggregate.StrategyDefinitionAggregate;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyProbabilityPreviewVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyReadinessIssueVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyReadinessReportVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyDefinitionRepository;
import com.lavyoung.marketforge.domain.strategy.service.IStrategyDefinitionReadinessChecker;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 策略发布就绪检查应用服务默认实现。
 *
 * <p>本服务编排完整策略定义查询和领域就绪检查，并负责将领域报告转换为
 * Application 结果。它不处理 HTTP 参数、统一响应包装或前端展示格式。</p>
 *
 * <p>就绪检查是只读用例，不修改策略状态，也不刷新概率快照。真正发布时
 * 仍由策略定义应用服务按照“校验、预热、状态条件更新”的顺序执行。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
@Service
@RequiredArgsConstructor
public class StrategyReadinessApplicationServiceImpl implements IStrategyReadinessApplicationService {

    private final IStrategyDefinitionRepository strategyDefinitionRepository;
    private final IStrategyDefinitionReadinessChecker readinessChecker;

    @Override
    @Transactional(readOnly = true)
    public StrategyReadinessResult check(Long strategyId) {
        Objects.requireNonNull(strategyId, "strategyId must not be null");
        StrategyDefinitionAggregate aggregate = strategyDefinitionRepository.queryByStrategyId(strategyId)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.STRATEGY_NOT_FOUND, strategyId));
        StrategyReadinessReportVO reportVO = readinessChecker.check(aggregate);
        return new StrategyReadinessResult(
                reportVO.strategyId(),
                toIssueResults(reportVO.errors()),
                toIssueResults(reportVO.warnings()),
                toIssueResults(reportVO.suggestions()),
                toPreviewResult(reportVO.probabilityPreview())
        );
    }

    /**
     * 批量转换策略检查问题。
     *
     * @param issues 领域问题集合
     * @return Application 问题结果集合
     */
    private List<StrategyReadinessIssueResult> toIssueResults(List<StrategyReadinessIssueVO> issues) {
        return issues.stream()
                .map(issue -> new StrategyReadinessIssueResult(issue.code(), issue.path(), issue.message()))
                .toList();
    }

    /**
     * 转换领域概率预览及其全部命中区间。
     *
     * @param preview 领域概率预览
     * @return Application 概率预览结果
     */
    private StrategyProbabilityPreviewResult toPreviewResult(StrategyProbabilityPreviewVO preview) {
        List<StrategyProbabilityIntervalResult> intervals = preview.intervals().stream()
                .map(interval -> new StrategyProbabilityIntervalResult(
                        interval.awardId(),
                        interval.weight(),
                        interval.startInclusive(),
                        interval.endExclusive(),
                        interval.probability()
                ))
                .toList();
        return new StrategyProbabilityPreviewResult(preview.available(), preview.totalWeight(), intervals);
    }
}
