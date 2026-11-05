package com.lavyoung.marketforge.application.activity.service.impl;

import com.lavyoung.marketforge.application.activity.model.ActivityReadinessIssueResult;
import com.lavyoung.marketforge.application.activity.model.ActivityReadinessResult;
import com.lavyoung.marketforge.application.activity.service.IActivityReadinessApplicationService;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityReadinessIssueVO;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityReadinessReportVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityDefinitionRepository;
import com.lavyoung.marketforge.domain.activity.service.IActivityDefinitionReadinessChecker;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 活动开放就绪检查应用服务默认实现。
 *
 * <p>该服务负责读取活动定义、取得统一业务时间、调用领域检查器，并将领域报告
 * 转换成 Application Result。活动、SKU、策略和概率规则仍由领域检查器负责。</p>
 *
 * <p>本用例只执行读取和检查，不修改活动状态，不执行策略预热，也不切换活动展台。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/05
 */
@Service
@RequiredArgsConstructor
public class ActivityReadinessApplicationServiceImpl implements IActivityReadinessApplicationService {

    private final IActivityDefinitionRepository activityDefinitionRepository;
    private final IActivityDefinitionReadinessChecker readinessChecker;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public ActivityReadinessResult check(Long activityId) {
        Objects.requireNonNull(activityId, "activityId must not be null");
        if (activityId <= 0) {
            throw new IllegalArgumentException("activityId must be positive");
        }
        LocalDateTime checkedAt = LocalDateTime.now(clock);
        ActivityDefinitionEntity definition = activityDefinitionRepository.queryByActivityId(activityId)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.LOTTERY_ACTIVITY_NOT_FOUND, activityId));
        ActivityReadinessReportVO reportVO = readinessChecker.check(definition, checkedAt);
        return new ActivityReadinessResult(
                definition.activityId(),
                checkedAt,
                toIssueResults(reportVO.errors()),
                toIssueResults(reportVO.warnings()),
                toIssueResults(reportVO.suggestions())
        );
    }

    /**
     * 批量转换活动检查问题。
     *
     * @param issues 领域问题集合
     * @return Application 问题结果集合
     */
    private List<ActivityReadinessIssueResult> toIssueResults(List<ActivityReadinessIssueVO> issues) {
        return issues.stream()
                .map(issue -> new ActivityReadinessIssueResult(issue.code(), issue.path(), issue.message()))
                .toList();
    }
}
