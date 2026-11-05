package com.lavyoung.marketforge.domain.activity.service.impl;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityDefinitionStateVO;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityReadinessIssueVO;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityReadinessReportVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityCountDefinitionRepository;
import com.lavyoung.marketforge.domain.activity.repository.IActivitySkuDefinitionRepository;
import com.lavyoung.marketforge.domain.activity.service.IActivityDefinitionReadinessChecker;
import com.lavyoung.marketforge.domain.strategy.model.aggregate.StrategyDefinitionAggregate;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyReadinessIssueVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyReadinessReportVO;
import com.lavyoung.marketforge.domain.strategy.repository.IStrategyDefinitionRepository;
import com.lavyoung.marketforge.domain.strategy.service.IStrategyDefinitionReadinessChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 活动配置运行就绪检查器默认实现。
 *
 * <p>该检查器负责聚合活动、次数配置、活动 SKU 和抽奖策略的只读状态，
 * 将阻止活动开放或展台发布的问题、运营风险和配置建议组织成统一报告。</p>
 *
 * <p>活动自身规则和关联对象规则使用同一次业务检查时间。次数配置使用批量查询，
 * 避免按照 SKU 数量产生 N+1 数据库访问。策略内部规则复用现有策略就绪检查器，
 * 保证活动检查和策略发布使用相同的概率、奖品及规则标准。</p>
 *
 * <p>该实现不修改任何聚合，也不预热 Redis。检查通过只代表配置满足运行条件，
 * 真正的活动状态迁移、策略预热和展台切换仍由 Application Service 编排。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/04
 */
@Service
@RequiredArgsConstructor
public class ActivityDefinitionReadinessChecker implements IActivityDefinitionReadinessChecker {

    private final IActivityCountDefinitionRepository activityCountDefinitionRepository;
    private final IActivitySkuDefinitionRepository activitySkuDefinitionRepository;
    private final IStrategyDefinitionRepository strategyDefinitionRepository;
    private final IStrategyDefinitionReadinessChecker strategyDefinitionReadinessChecker;

    @Override
    public ActivityReadinessReportVO check(ActivityDefinitionEntity definition, LocalDateTime checkedAt) {
        Objects.requireNonNull(definition, "definition must not be null");
        Objects.requireNonNull(checkedAt, "checkedAt must not be null");

        ReadinessIssues issues = ReadinessIssues.empty();
        // 收集活动的问题
        collectActivityIssues(definition, checkedAt, issues);

        List<ActivitySkuDefinitionEntity> skuDefinitions = activitySkuDefinitionRepository.queryByActivityId(definition.activityId());
        Map<Long, ActivityCountDefinitionEntity> countDefinitions = queryCountDefinitions(definition, skuDefinitions);
        ActivityReferenceSnapshot referenceSnapshot = new ActivityReferenceSnapshot(definition, skuDefinitions, countDefinitions);

        collectActivityCountIssues(referenceSnapshot, issues);
        collectSkuIssues(referenceSnapshot, issues);
        collectStrategyIssues(definition, issues);

        return new ActivityReadinessReportVO(
                definition.activityId(),
                issues.errors(),
                issues.warnings(),
                issues.suggestions()
        );
    }

    /**
     * 检查活动生命周期状态和有效时间。
     *
     * <p>草稿活动执行该检查，是为了判断其能否开放；开放活动执行该检查，
     * 是为了判断其能否继续进入展台预热和发布流程。因此，就绪检查器允许
     * 草稿和开放两种状态，具体操作所要求的精确状态由对应应用服务校验。</p>
     *
     * <p>关闭状态是不可逆终态，不能再次开放或发布。活动结束时间已经到达时
     * 必须阻止后续操作；活动已经开始但尚未结束时允许继续，不过需要提醒
     * 运营人员活动将在发布后立即对用户生效。</p>
     *
     * @param definition 待检查的活动定义
     * @param checkedAt  本次检查采用的业务时间
     * @param issues     用于收集阻断错误、警告和建议的问题上下文
     */
    private void collectActivityIssues(ActivityDefinitionEntity definition, LocalDateTime checkedAt, ReadinessIssues issues) {
        if (definition.state() == ActivityDefinitionStateVO.CLOSE) {
            issues.error(
                    "activity.state.closed",
                    "state",
                    "活动已经关闭，不能再次开放或发布"
            );
        }

        if (!definition.endDateTime().isAfter(checkedAt)) {
            issues.error(
                    "activity.time.expired",
                    "endDateTime",
                    "活动结束时间已经到达，不能继续开放或发布"
            );
            return;
        }

        if (!definition.beginDateTime().isAfter(checkedAt)) {
            issues.warning(
                    "activity.time.already-started",
                    "beginDateTime",
                    "活动开始时间已经到达，发布后将立即允许用户参与"
            );
        }
    }

    /**
     * 批量读取活动和全部 SKU 引用的次数配置。
     *
     * <p>活动自身的次数配置和 SKU 次数配置可能不同，因此先将全部业务标识
     * 合并并去重，再执行一次批量查询。</p>
     *
     * @param definition     活动定义
     * @param skuDefinitions 活动 SKU 定义
     * @return 以次数配置业务标识为键的配置映射
     */
    private Map<Long, ActivityCountDefinitionEntity> queryCountDefinitions(ActivityDefinitionEntity definition,
                                                                           List<ActivitySkuDefinitionEntity> skuDefinitions) {
        Set<Long> activityCountIds = Stream.concat(Stream.of(definition.activityCountId()),
                        skuDefinitions.stream().map(ActivitySkuDefinitionEntity::activityCountId))
                .collect(Collectors.toUnmodifiableSet());
        return activityCountDefinitionRepository.queryByActivityCountIds(activityCountIds).stream()
                .collect(Collectors.toUnmodifiableMap(ActivityCountDefinitionEntity::activityCountId, Function.identity()));
    }

    /**
     * 检查活动自身引用的次数配置是否存在。
     *
     * @param referenceSnapshot 活动及其关联配置的只读快照
     * @param issues            问题收集器
     */
    private void collectActivityCountIssues(ActivityReferenceSnapshot referenceSnapshot, ReadinessIssues issues) {
        ActivityDefinitionEntity definition = referenceSnapshot.definition();
        if (!referenceSnapshot.countDefinitions().containsKey(definition.activityCountId())) {
            issues.error(
                    "activity.count.missing",
                    "activityCountId",
                    "活动引用的次数配置不存在"
            );
        }
    }

    /**
     * 检查活动 SKU 的归属、次数配置引用和可用库存。
     *
     * <p>单个库存为零的 SKU 只产生警告，因为同一活动可能还有其他可用 SKU；
     * 只有全部 SKU 都不可用时，才增加阻止活动开放的错误。</p>
     *
     * @param referenceSnapshot 活动及其关联配置的只读快照
     * @param issues            问题收集器
     */
    private void collectSkuIssues(ActivityReferenceSnapshot referenceSnapshot, ReadinessIssues issues) {
        ActivityDefinitionEntity definition = referenceSnapshot.definition();
        List<ActivitySkuDefinitionEntity> skuDefinitions = referenceSnapshot.skuDefinitions();
        Map<Long, ActivityCountDefinitionEntity> countDefinitions = referenceSnapshot.countDefinitions();

        if (skuDefinitions.isEmpty()) {
            issues.error(
                    "activity.sku.missing",
                    "skus",
                    "活动至少需要配置一个额度 SKU"
            );
            return;
        }
        boolean hasUsableSku = false;
        for (int index = 0; index < skuDefinitions.size(); index++) {
            ActivitySkuDefinitionEntity sku = skuDefinitions.get(index);
            String path = "skus[" + index + "]";
            boolean correctActivity = definition.activityId().equals(sku.activityId());
            boolean countExists = countDefinitions.containsKey(sku.activityCountId());
            boolean stockAvailable = sku.stockCountSurplus() > 0;

            if (!correctActivity) {
                issues.error(
                        "activity.sku.activity-mismatch",
                        path + ".activityId",
                        "SKU 不属于当前活动"
                );
            }

            if (!countExists) {
                issues.error(
                        "activity.sku.count-missing",
                        path + ".activityCountId",
                        "SKU 引用的次数配置不存在"
                );
            }

            if (!stockAvailable) {
                issues.warning(
                        "activity.sku.stock-empty",
                        path + ".stockCountSurplus",
                        "SKU 当前没有剩余库存，用户无法通过该 SKU 获得活动额度"
                );
            }

            if (!sku.supportsCreditExchange()) {
                issues.suggestion(
                        "activity.sku.credit-exchange-disabled",
                        path + ".creditAmount",
                        "该 SKU 未配置积分价格，只能通过签到、返利等非积分场景发放"
                );
            }

            hasUsableSku = hasUsableSku || correctActivity && countExists && stockAvailable;
        }

        if (!hasUsableSku) {
            issues.error(
                    "activity.sku.none-usable",
                    "skus",
                    "活动没有归属、次数配置和库存均有效的可用 SKU"
            );
        }
    }

    /**
     * 将策略问题映射到活动报告中的策略配置路径。
     *
     * @param issue 策略就绪问题
     * @return 活动就绪问题
     */
    private ActivityReadinessIssueVO toActivityIssue(StrategyReadinessIssueVO issue) {
        String path = "strategy".equals(issue.path()) ? "strategy" : "strategy." + issue.path();
        return new ActivityReadinessIssueVO(issue.code(), path, issue.message());
    }

    /**
     * 检查活动引用的策略状态，并合并策略自己的就绪报告。
     *
     * <p>策略不存在或未生效会直接阻止活动开放。策略存在时继续调用策略就绪
     * 检查器，复用奖品、概率空间、权重子奖池和规则模板检查，不在活动服务中
     * 重复实现策略规则。</p>
     *
     * @param definition 活动定义
     * @param issues     问题收集器
     */
    private void collectStrategyIssues(ActivityDefinitionEntity definition, ReadinessIssues issues) {
        StrategyDefinitionAggregate strategy = strategyDefinitionRepository.queryByStrategyId(definition.strategyId()).orElse(null);

        if (strategy == null) {
            issues.error(
                    "activity.strategy.missing",
                    "strategyId",
                    "活动引用的抽奖策略不存在"
            );
            return;
        }

        if (!strategy.canReference()) {
            issues.error(
                    "activity.strategy.not-active",
                    "strategyId",
                    "活动引用的抽奖策略尚未生效或已经停用"
            );
        }
        StrategyReadinessReportVO readinessReportVO = strategyDefinitionReadinessChecker.check(strategy);
        mergeStrategyIssues(readinessReportVO, issues);
    }

    /**
     * 将策略就绪报告转换为活动报告中的策略配置问题。
     *
     * @param strategyReport 策略就绪报告
     * @param issues         活动问题收集器
     */
    private void mergeStrategyIssues(StrategyReadinessReportVO strategyReport, ReadinessIssues issues) {
        strategyReport.errors().stream()
                .map(this::toActivityIssue)
                .forEach(issues.errors()::add);
        strategyReport.warnings().stream()
                .map(this::toActivityIssue)
                .forEach(issues.warnings()::add);
        strategyReport.suggestions().stream()
                .map(this::toActivityIssue)
                .forEach(issues.suggestions()::add);
    }

    /**
     * 活动及其关联配置的一次只读查询快照。
     *
     * <p>快照将活动定义、活动 SKU 和批量读取的次数配置作为一个完整检查上下文，
     * 防止检查方法参数持续膨胀。集合在构造时复制，避免检查期间被外部修改。</p>
     *
     * @param definition       活动定义x
     * @param skuDefinitions   当前活动全部 SKU
     * @param countDefinitions 活动和 SKU 引用的次数配置
     */
    private record ActivityReferenceSnapshot(
            ActivityDefinitionEntity definition,
            List<ActivitySkuDefinitionEntity> skuDefinitions,
            Map<Long, ActivityCountDefinitionEntity> countDefinitions
    ) {

        private ActivityReferenceSnapshot {
            Objects.requireNonNull(definition, "definition must not be null");
            skuDefinitions = List.copyOf(Objects.requireNonNull(skuDefinitions, "skuDefinitions must not be null"));
            countDefinitions = Map.copyOf(Objects.requireNonNull(countDefinitions, "countDefinitions must not be null"));
        }
    }

    /**
     * 活动就绪问题的可变收集上下文。
     *
     * <p>收集期间使用可变列表累积多条问题；创建最终
     * {@link ActivityReadinessReportVO} 时会统一执行不可变复制。</p>
     *
     * @param errors      阻止活动开放的错误
     * @param warnings    不阻止开放的风险提示
     * @param suggestions 配置优化建议
     */
    private record ReadinessIssues(
            List<ActivityReadinessIssueVO> errors,
            List<ActivityReadinessIssueVO> warnings,
            List<ActivityReadinessIssueVO> suggestions
    ) {
        private static ReadinessIssues empty() {
            return new ReadinessIssues(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        private void error(String code, String path, String message) {
            errors.add(new ActivityReadinessIssueVO(code, path, message));
        }

        private void warning(String code, String path, String message) {
            warnings.add(new ActivityReadinessIssueVO(code, path, message));
        }

        private void suggestion(String code, String path, String message) {
            suggestions.add(new ActivityReadinessIssueVO(code, path, message));
        }
    }
}
