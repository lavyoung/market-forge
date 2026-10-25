package com.lavyoung.marketforge.domain.strategy.service.impl;

import com.lavyoung.marketforge.domain.award.model.entity.AwardDefinitionEntity;
import com.lavyoung.marketforge.domain.award.repository.IAwardDefinitionRepository;
import com.lavyoung.marketforge.domain.strategy.model.aggregate.StrategyDefinitionAggregate;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.domain.strategy.service.IStrategyDefinitionPublicationValidator;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.algorithm.ProbabilitySpaceFactory;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 策略定义发布就绪校验器默认实现。
 *
 * <p>校验按照“奖池结构、概率空间、规则配置、奖品定义”的顺序执行。
 * 先完成不需要数据库访问的结构检查，只有本地结构合法时才批量读取奖品定义，
 * 减少明显错误配置对数据库造成的无效访问。</p>
 *
 * <p>概率合法性复用运行时使用的 {@link ProbabilitySpaceFactory}，确保后台发布校验
 * 和真实抽奖装配采用同一套权重语义，避免发布时通过、运行时失败。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@Service
@RequiredArgsConstructor
public class StrategyDefinitionPublicationValidator implements IStrategyDefinitionPublicationValidator {

    private static final int STRATEGY_RULE_TYPE = 1;
    private static final int AWARD_RULE_TYPE = 2;

    private final IAwardDefinitionRepository awardDefinitionRepository;
    private final ProbabilitySpaceFactory probabilitySpaceFactory;

    /**
     * StrategyDefinitionAggregate
     * │
     * ├── awards ──> ProbabilitySpaceFactory
     * │                    └── 验证权重能否建立概率空间
     * │
     * ├── rules ───> PublicationValidator
     * │                    ├── 规则类型
     * │                    ├── 奖品归属
     * │                    └── 权重档位引用
     * │
     * └── awardIds ─> IAwardDefinitionRepository.queryByAwardIds
     * └── 一条 IN SQL
     * └── 校验奖品存在且 active
     *
     * @param aggregate 待发布的完整策略定义聚合
     */
    @Override
    public void validate(StrategyDefinitionAggregate aggregate) {
        Objects.requireNonNull(aggregate, "aggregate must not be null");
        validateAwards(aggregate);
        probabilitySpaceFactory.create(String.valueOf(aggregate.strategyId()), aggregate.awards());
        validateRules(aggregate);
        validateAwardDefinitions(aggregate);
    }

    /**
     * 校验策略奖品的库存、展示信息和顺序。
     *
     * <p>奖品标识重复和子配置归属已经由聚合构造器保证。这里补充只有发布时才需要
     * 强制满足的条件：奖池非空、库存可用、标题有效以及展示顺序唯一。</p>
     *
     * @param aggregate 待发布策略聚合
     * @throws BusinessException 奖品结构不满足发布要求时抛出
     */
    private void validateAwards(StrategyDefinitionAggregate aggregate) {
        if (aggregate.awards().isEmpty()) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_AWARD_NOT_CONFIGURED, aggregate.strategyId());
        }
        Set<Integer> sorts = new HashSet<>();
        for (StrategyAwardEntity award : aggregate.awards()) {
            if (award.awardTitle() == null || award.awardTitle().isBlank()) {
                throw invalidConfig(aggregate.strategyId(), award.awardId(), "awardTitle");
            }
            if (award.awardCount() == null || award.awardCount() <= 0) {
                throw invalidConfig(aggregate.strategyId(), award.awardId(), "awardCount");
            }
            if (award.awardCountSurplus() == null || award.awardCountSurplus() <= 0
                    || award.awardCountSurplus() > award.awardCount()) {
                throw invalidConfig(aggregate.strategyId(), award.awardId(), "awardCountSurplus");
            }
            if (award.sort() == null || award.sort() <= 0 || !sorts.add(award.sort())) {
                throw invalidConfig(aggregate.strategyId(), award.awardId(), "sort");
            }

            parseRuleModels(award.ruleModels(), Constants.SPLIT, aggregate.strategyId(), award.awardId());
        }
    }

    /**
     * 校验策略级规则和奖品级规则的类型、归属、模型和值。
     *
     * <p>策略级规则不能绑定奖品；奖品级规则必须绑定当前奖池中的奖品。
     * 同一作用域内不允许重复配置同一种规则。</p>
     *
     * @param aggregate 待发布策略聚合
     * @throws BusinessException 规则配置不合法时抛出
     */
    private void validateRules(StrategyDefinitionAggregate aggregate) {
        Set<Long> awardIds = aggregate.awards().stream()
                .map(StrategyAwardEntity::awardId)
                .collect(Collectors.toUnmodifiableSet());
        parseRuleModels(aggregate.ruleModels(), ",", aggregate.strategyId(), "strategy");
        Map<Long, Set<String>> awardRuleModels = aggregate.awards().stream()
                .collect(Collectors.toUnmodifiableMap(
                        StrategyAwardEntity::awardId,
                        award -> parseRuleModels(
                                award.ruleModels(),
                                Constants.SPLIT,
                                aggregate.strategyId(),
                                award.awardId()
                        )
                ));
        Set<String> uniqueRules = new HashSet<>();
        for (StrategyRuleEntity rule : aggregate.rules()) {
            validateRuleOwnership(aggregate.strategyId(), rule, awardIds);

            RuleModel ruleModel = RuleModel.get(rule.ruleModel());
            if (ruleModel == null || ruleModel == RuleModel.DEFAULT) {
                throw invalidConfig(aggregate.strategyId(), rule.awardId(), rule.ruleModel());
            }
            if (rule.ruleValue() == null || rule.ruleValue().isBlank()) {
                throw invalidConfig(aggregate.strategyId(), rule.awardId(), "ruleValue");
            }
            String uniqueKey = rule.ruleType() + ":" + rule.awardId() + ":" + rule.ruleModel();
            if (!uniqueRules.add(uniqueKey)) {
                throw invalidConfig(aggregate.strategyId(), rule.awardId(), "duplicated rule " + rule.ruleModel());
            }
            if (rule.ruleType() == AWARD_RULE_TYPE && !awardRuleModels.get(rule.awardId()).contains(rule.ruleModel())) {
                throw invalidConfig(aggregate.strategyId(), rule.awardId(), "rule model is not declared by award");
            }
            if (ruleModel == RuleModel.WEIGHT) {
                validateWeightRule(aggregate.strategyId(), rule, awardIds);
            }
        }
    }

    /**
     * 校验一条规则与策略或奖品的归属关系。
     *
     * @param strategyId 策略业务标识
     * @param rule       待校验规则
     * @param awardIds   当前策略全部奖品标识
     * @throws BusinessException 规则类型或归属不合法时抛出
     */
    private void validateRuleOwnership(Long strategyId, StrategyRuleEntity rule, Set<Long> awardIds) {
        if (rule.ruleType() == null) {
            throw invalidConfig(strategyId, rule.awardId(), "ruleType");
        }

        if (rule.ruleType() == STRATEGY_RULE_TYPE && rule.awardId() != null) {
            throw invalidConfig(strategyId, rule.awardId(), "strategy rule must not bind award");
        }

        if (rule.ruleType() == AWARD_RULE_TYPE && (rule.awardId() == null || !awardIds.contains(rule.awardId()))) {
            throw invalidConfig(strategyId, rule.awardId(), "award rule references unknown award");
        }

        if (rule.ruleType() != STRATEGY_RULE_TYPE && rule.ruleType() != AWARD_RULE_TYPE) {
            throw invalidConfig(strategyId, rule.awardId(), "unsupported ruleType " + rule.ruleType());
        }
    }

    /**
     * 校验权重规则中的候选奖品全部属于当前策略。
     *
     * <p>运行时会根据权重档位过滤奖池。如果规则引用了不存在的奖品而未在发布时拒绝，
     * 运行时过滤会静默忽略错误标识，使运营配置与实际奖池不一致。</p>
     *
     * @param strategyId 策略业务标识
     * @param rule       权重规则
     * @param awardIds   当前策略全部奖品标识
     * @throws BusinessException 权重规则格式错误、候选集合为空或引用未知奖品时抛出
     */
    private void validateWeightRule(Long strategyId, StrategyRuleEntity rule, Set<Long> awardIds) {
        try {
            Map<String, List<Long>> weightValues = rule.ruleWeightValues();
            if (weightValues.isEmpty()) {
                throw invalidConfig(strategyId, rule.awardId(), "weight rule is empty");
            }
            weightValues.forEach((weight, eligibleAwardIds) -> {
                if (eligibleAwardIds.isEmpty() || !awardIds.containsAll(eligibleAwardIds)) {
                    throw invalidConfig(strategyId, rule.awardId(), "weight " + weight + " references unknown award");
                }
            });
        } catch (NumberFormatException | IndexOutOfBoundsException exception) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_CONFIG_INVALID, exception,
                    strategyId, rule.ruleModel(), rule.ruleValue());
        }
    }

    /**
     * 批量校验策略引用的所有奖品定义存在且已经启用。
     *
     * @param aggregate 待发布策略聚合
     * @throws BusinessException 奖品定义不存在或未启用时抛出
     */
    private void validateAwardDefinitions(StrategyDefinitionAggregate aggregate) {
        List<Long> awardIds = aggregate.awards().stream().map(StrategyAwardEntity::awardId).toList();
        Map<Long, AwardDefinitionEntity> definitionEntityMap = awardDefinitionRepository.queryByAwardIds(awardIds).stream()
                .collect(Collectors.toUnmodifiableMap(AwardDefinitionEntity::awardId, Function.identity()));
        for (Long awardId : awardIds) {
            AwardDefinitionEntity definition = definitionEntityMap.get(awardId);
            if (definition == null) {
                throw BusinessException.of(BusinessResponseCode.AWARD_NOT_FOUND, awardId);
            }
            if (!definition.canReference()) {
                throw BusinessException.of(BusinessResponseCode.AWARD_NOT_AVAILABLE,
                        definition.awardId(), definition.state().getCode());
            }
        }
    }

    /**
     * 解析并校验规则模型编码集合。
     *
     * @param value      规则模型编码字符串
     * @param delimiter  当前字段使用的分隔符
     * @param strategyId 策略业务标识
     * @param owner      策略或奖品上下文
     * @return 去重后的规则模型编码集合
     * @throws BusinessException 编码未知、包含 default 或重复时抛出
     */
    private Set<String> parseRuleModels(String value, String delimiter, Long strategyId, Object owner) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        List<String> models = Arrays.stream(value.split(Pattern.quote(delimiter)))
                .map(String::trim)
                .filter(m -> !m.isEmpty())
                .toList();
        Set<String> uniqueModels = new HashSet<>();
        for (String model : models) {
            RuleModel ruleModel = RuleModel.get(model);
            if (ruleModel == null || ruleModel == RuleModel.DEFAULT || !uniqueModels.add(model)) {
                throw invalidConfig(strategyId, owner, model);
            }
        }
        return Set.copyOf(uniqueModels);
    }

    /**
     * 创建统一的策略配置非法异常。
     *
     * @param strategyId 策略业务标识
     * @param owner      策略或奖品上下文
     * @param reason     不满足发布要求的字段或原因
     * @return 策略配置非法业务异常
     */
    private BusinessException invalidConfig(Long strategyId, Object owner, Object reason) {
        return BusinessException.of(BusinessResponseCode.STRATEGY_CONFIG_INVALID, strategyId, owner, reason);
    }
}
