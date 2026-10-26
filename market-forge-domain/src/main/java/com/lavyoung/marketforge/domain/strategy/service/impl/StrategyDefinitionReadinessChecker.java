package com.lavyoung.marketforge.domain.strategy.service.impl;

import com.lavyoung.marketforge.domain.strategy.model.aggregate.StrategyDefinitionAggregate;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyProbabilityPreviewVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyReadinessIssueVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyReadinessReportVO;
import com.lavyoung.marketforge.domain.strategy.service.IStrategyDefinitionPublicationValidator;
import com.lavyoung.marketforge.domain.strategy.service.IStrategyDefinitionReadinessChecker;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.StrategyProbabilityPreviewFactory;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 策略定义发布就绪检查器默认实现。
 *
 * <p>检查器复用现有发布校验器作为阻断规则的唯一来源，避免后台预检
 * 与真实发布使用不同的校验标准。校验成功后，再通过正式概率空间工厂
 * 生成奖品命中区间预览。</p>
 *
 * <p>当前发布校验器采用失败即停止模式，因此第一版每次检查最多返回
 * 一个阻断错误。警告和建议属于非阻断信息，可以在同一报告中返回。
 * 后续将发布校验规则拆分为可累计的规则组件后，可以在不修改接口契约
 * 的情况下返回全部阻断错误。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@Service
@RequiredArgsConstructor
public class StrategyDefinitionReadinessChecker implements IStrategyDefinitionReadinessChecker {

    private final IStrategyDefinitionPublicationValidator validator;
    private final StrategyProbabilityPreviewFactory previewFactory;

    @Override
    public StrategyReadinessReportVO check(StrategyDefinitionAggregate aggregate) {
        Objects.requireNonNull(aggregate, "aggregate must not be null");

        List<StrategyReadinessIssueVO> warnings = collectWarnings(aggregate);
        List<StrategyReadinessIssueVO> suggestions = collectSuggestions(aggregate);

        try {
            validator.validate(aggregate);
            StrategyProbabilityPreviewVO probabilityPreviewVO = previewFactory.create(String.valueOf(aggregate.strategyId()), aggregate.awards());
            return new StrategyReadinessReportVO(
                    aggregate.strategyId(),
                    List.of(),
                    warnings,
                    suggestions,
                    probabilityPreviewVO
            );
        } catch (BusinessException exception) {
            return new StrategyReadinessReportVO(
                    aggregate.strategyId(),
                    List.of(toReadinessIssue(exception)),
                    warnings,
                    suggestions,
                    StrategyProbabilityPreviewVO.unavailable()
            );
        }
    }

    /**
     * 收集不阻止发布但需要运营人员关注的风险。
     *
     * <p>策略草稿通常应该使用完整初始库存。如果剩余库存已经小于库存总量，
     * 说明该配置可能来自运行中的历史策略，运营人员需要确认是否确实希望
     * 使用剩余库存继续发布。</p>
     *
     * @param aggregate 待检查策略定义
     * @return 策略风险警告集合
     */
    private List<StrategyReadinessIssueVO> collectWarnings(StrategyDefinitionAggregate aggregate) {
        List<StrategyReadinessIssueVO> warnings = new ArrayList<>();
        for (int index = 0; index < aggregate.awards().size(); index++) {
            StrategyAwardEntity award = aggregate.awards().get(index);
            if (award.awardCount() != null && award.awardCountSurplus() != null
                    && award.awardCountSurplus() < award.awardCount()) {
                warnings.add(new StrategyReadinessIssueVO(
                        "strategy.award.stock-already-consumed",
                        "awards[" + index + "].awardCountSurplus",
                        "奖品剩余库存小于库存总量，请确认是否使用已消耗库存继续发布"
                ));
            }
        }
        return warnings;
    }

    /**
     * 收集不影响策略正确性的运营优化建议。
     *
     * <p>奖品副标题不是抽奖必填字段，但缺失时会降低用户端奖品说明的完整性。
     * 单奖品策略在技术上合法，但不具备随机分配意义，因此作为建议提示。</p>
     *
     * @param aggregate 待检查策略定义
     * @return 策略配置优化建议集合
     */
    private List<StrategyReadinessIssueVO> collectSuggestions(StrategyDefinitionAggregate aggregate) {
        List<StrategyReadinessIssueVO> suggestions = new ArrayList<>();
        for (int index = 0; index < aggregate.awards().size(); index++) {
            StrategyAwardEntity award = aggregate.awards().get(index);
            if (award.awardSubtitle() == null || award.awardSubtitle().isBlank()) {
                suggestions.add(new StrategyReadinessIssueVO(
                        "strategy.award.subtitle-missing",
                        "awards[" + index + "].awardSubtitle",
                        "建议补充奖品副标题，帮助用户理解奖品内容"
                ));
            }
        }
        if (aggregate.awards().size() == 1) {
            suggestions.add(new StrategyReadinessIssueVO(
                    "strategy.single-award",
                    "awards",
                    "当前策略只有一个奖品，不具备随机分配效果"
            ));
        }
        return suggestions;
    }

    /**
     * 将发布校验业务异常转换成后台可展示的阻断错误。
     *
     * <p>错误编码使用现有国际化资源键，保证前端识别值稳定；错误路径根据
     * 业务响应类型归类，用于后台表单定位对应配置区域。</p>
     *
     * @param exception 发布校验产生的业务异常
     * @return 策略就绪检查错误
     */
    private StrategyReadinessIssueVO toReadinessIssue(BusinessException exception) {
        return new StrategyReadinessIssueVO(
                exception.getI18nKey(),
                resolveIssuePath(exception),
                exception.getMessage()
        );
    }

    /**
     * 根据业务错误类型定位后台策略表单字段。
     *
     * @param exception 发布校验产生的业务异常
     * @return 对应的策略配置路径
     */
    private String resolveIssuePath(BusinessException exception) {
        if (!(exception.getResponseCode() instanceof BusinessResponseCode responseCode)) {
            return "strategy";
        }
        return switch (responseCode) {
            case STRATEGY_AWARD_NOT_CONFIGURED -> "awards";
            case STRATEGY_RATE_INVALID, STRATEGY_RATE_TOTAL_INVALID -> "awards[*].awardRate";
            case STRATEGY_RULE_VALUE_INVALID -> "rules[*].ruleValue";
            case AWARD_NOT_FOUND, AWARD_NOT_AVAILABLE -> "awards[*].awardId";
            default -> "strategy";
        };
    }
}
