package com.lavyoung.marketforge.domain.strategy.service.impl;

import com.lavyoung.marketforge.domain.award.model.entity.AwardDefinitionEntity;
import com.lavyoung.marketforge.domain.award.repository.IAwardDefinitionRepository;
import com.lavyoung.marketforge.domain.strategy.model.aggregate.StrategyDefinitionAggregate;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.BlacklistRuleValueVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.LockRuleValueVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.WeightRuleValueVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.WeightTierVO;
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

    /**
     * 当前责任链已经注册的策略级规则。
     */
    private static final Set<String> ALLOWED_STRATEGY_RULE_MODELS = Set.of(RuleModel.RULE_BLACKLIST.getCode(),
            RuleModel.WEIGHT.getCode());
    /**
     * 当前系统规则树模板允许作为奖品入口的规则。
     *
     * <p>幸运奖是规则树内部兜底节点，不由策略奖品直接声明。</p>
     */
    private static final Set<String> ALLOWED_AWARD_RULE_MODELS = Set.of(RuleModel.LOCK.getCode(),
            RuleModel.RULE_STOCK.getCode());
    /**
     * 必须在 strategy_rule 表中提供参数的奖品规则。
     *
     * <p>库存规则直接读取策略库存，不需要额外规则值；次数锁需要配置解锁次数。</p>
     */
    private static final Set<String> REQUIRED_AWARD_RULE_CONFIGS = Set.of(RuleModel.LOCK.getCode());

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
     * 校验策略奖品的库存、展示信息、顺序和规则树入口。
     *
     * <p>奖品标识重复和子配置归属由聚合构造器保证。发布阶段进一步要求奖池非空、
     * 库存可用、顺序唯一，并且奖品规则只能使用系统已经提供规则树模板的入口。</p>
     *
     * @param aggregate 待发布策略聚合
     * @throws BusinessException 奖品结构或规则树入口不满足发布要求时抛出
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
            Set<String> awardModels = parseRuleModels(award.ruleModels(), Constants.SPLIT, aggregate.strategyId(), award.awardId());
            if (!ALLOWED_AWARD_RULE_MODELS.containsAll(awardModels)) {
                Set<String> unsupportedModels = new HashSet<>(awardModels);
                unsupportedModels.removeAll(ALLOWED_AWARD_RULE_MODELS);
                throw invalidConfig(aggregate.strategyId(), award.awardId(),
                        "unsupported award rule models " + unsupportedModels);
            }
        }
    }

    /**
     * 校验规则声明、规则配置、作用域和参数值。
     *
     * <p>策略根配置和奖品配置负责声明运行时需要执行的规则，strategy_rule 负责保存
     * 需要参数的规则配置。两者必须匹配，防止出现“声明了但没参数”或“配置了但不会执行”
     * 的无效数据。</p>
     *
     * @param aggregate 待发布策略聚合
     * @throws BusinessException 规则声明、作用域或参数不合法时抛出
     */
    private void validateRules(StrategyDefinitionAggregate aggregate) {
        Set<Long> awardIds = aggregate.awards().stream()
                .map(StrategyAwardEntity::awardId)
                .collect(Collectors.toUnmodifiableSet());

        Set<String> strategyModels = parseRuleModels(
                aggregate.ruleModels(),
                ",",
                aggregate.strategyId(),
                "strategy"
        );
        if (!ALLOWED_STRATEGY_RULE_MODELS.containsAll(strategyModels)) {
            Set<String> unsupportedModels = new HashSet<>(strategyModels);
            unsupportedModels.removeAll(ALLOWED_STRATEGY_RULE_MODELS);
            throw invalidConfig(
                    aggregate.strategyId(),
                    "strategy",
                    "unsupported strategy rule models " + unsupportedModels
            );
        }

        Map<Long, Set<String>> awardModels = aggregate.awards().stream()
                .collect(Collectors.toUnmodifiableMap(
                        StrategyAwardEntity::awardId,
                        award -> parseRuleModels(
                                award.ruleModels(),
                                Constants.SPLIT,
                                aggregate.strategyId(),
                                award.awardId()
                        )
                ));

        RuleDeclarations declarations = new RuleDeclarations(strategyModels, awardModels);
        Set<String> configuredStrategyModels = new HashSet<>();
        Map<Long, Set<String>> configuredAwardModels = awardIds.stream()
                .collect(Collectors.toMap(Function.identity(), ignored -> new HashSet<>()));
        ConfiguredRules configuredRules = new ConfiguredRules(configuredStrategyModels, configuredAwardModels);

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
            if (rule.ruleDesc() == null || rule.ruleDesc().isBlank()) {
                throw invalidConfig(aggregate.strategyId(), rule.awardId(), "ruleDesc");
            }

            String uniqueKey = rule.ruleType() + ":" + rule.awardId() + ":" + rule.ruleModel();
            if (!uniqueRules.add(uniqueKey)) {
                throw invalidConfig(
                        aggregate.strategyId(),
                        rule.awardId(),
                        "duplicated rule " + rule.ruleModel()
                );
            }

            validateRuleDeclaration(aggregate.strategyId(), rule, declarations);
            validateRuleValue(aggregate.strategyId(), rule, awardIds);

            if (rule.ruleType() == STRATEGY_RULE_TYPE) {
                configuredRules.strategyModels().add(rule.ruleModel());
            } else {
                configuredRules.awardModels().get(rule.awardId()).add(rule.ruleModel());
            }
        }

        validateRequiredRuleConfigurations(aggregate.strategyId(), declarations, configuredRules);
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
     * 校验规则配置是否已经在正确作用域声明。
     *
     * @param strategyId   策略业务标识
     * @param rule         规则配置
     * @param declarations 策略和奖品的规则声明
     * @throws BusinessException 规则未声明或作用域不支持时抛出
     */
    private void validateRuleDeclaration(Long strategyId,
                                         StrategyRuleEntity rule,
                                         RuleDeclarations declarations) {
        if (rule.ruleType() == STRATEGY_RULE_TYPE) {
            if (!declarations.strategyModels().contains(rule.ruleModel())) {
                throw invalidConfig(strategyId, "strategy",
                        "rule configuration is not declared: " + rule.ruleModel());
            }
            return;
        }

        Set<String> declaredAwardModels = declarations.awardModels().get(rule.awardId());
        if (declaredAwardModels == null || !declaredAwardModels.contains(rule.ruleModel())) {
            throw invalidConfig(strategyId, rule.awardId(),
                    "rule configuration is not declared by award: " + rule.ruleModel());
        }
        if (!REQUIRED_AWARD_RULE_CONFIGS.contains(rule.ruleModel())) {
            throw invalidConfig(strategyId, rule.awardId(),
                    "rule does not accept strategy_rule configuration: " + rule.ruleModel());
        }
    }

    /**
     * 校验所有需要参数的规则都已经提供配置。
     *
     * @param strategyId      策略业务标识
     * @param declarations    规则声明
     * @param configuredRules 已配置规则
     * @throws BusinessException 规则声明缺少参数配置时抛出
     */
    private void validateRequiredRuleConfigurations(Long strategyId,
                                                    RuleDeclarations declarations,
                                                    ConfiguredRules configuredRules) {
        Set<String> missingStrategyModels = new HashSet<>(declarations.strategyModels());
        missingStrategyModels.removeAll(configuredRules.strategyModels());
        if (!missingStrategyModels.isEmpty()) {
            throw invalidConfig(strategyId, "strategy",
                    "missing strategy rule configurations " + missingStrategyModels);
        }

        declarations.awardModels().forEach((awardId, declaredModels) -> {
            Set<String> requiredModels = declaredModels.stream()
                    .filter(REQUIRED_AWARD_RULE_CONFIGS::contains)
                    .collect(Collectors.toSet());
            requiredModels.removeAll(configuredRules.awardModels().get(awardId));
            if (!requiredModels.isEmpty()) {
                throw invalidConfig(strategyId, awardId,
                        "missing award rule configurations " + requiredModels);
            }
        });
    }

    /**
     * 按规则类型校验规则值格式。
     *
     * @param strategyId 策略业务标识
     * @param rule       规则配置
     * @param awardIds   当前奖池奖品标识
     * @throws BusinessException 规则值格式或引用不合法时抛出
     */
    private void validateRuleValue(Long strategyId, StrategyRuleEntity rule, Set<Long> awardIds) {
        RuleModel ruleModel = RuleModel.get(rule.ruleModel());
        switch (ruleModel) {
            case WEIGHT -> validateWeightRule(strategyId, rule, awardIds);
            case RULE_BLACKLIST -> validateBlacklistRule(strategyId, rule, awardIds);
            case LOCK -> validateLockRule(strategyId, rule);
            default -> throw invalidConfig(strategyId, rule.awardId(),
                    "unsupported configurable rule " + rule.ruleModel());
        }
    }

    /**
     * 校验黑名单规则格式及其兜底奖品引用。
     *
     * <p>规则值自身的格式和用户列表约束由 {@link BlacklistRuleValueVO} 负责；
     * 本方法只补充聚合范围约束，确保兜底奖品属于当前策略奖池。</p>
     *
     * @param strategyId 策略业务标识
     * @param rule       黑名单规则配置
     * @param awardIds   当前策略奖池中的奖品标识
     * @throws BusinessException 规则值格式非法或兜底奖品不属于当前奖池时抛出
     */
    private void validateBlacklistRule(Long strategyId, StrategyRuleEntity rule, Set<Long> awardIds) {
        BlacklistRuleValueVO ruleValue;
        try {
            ruleValue = BlacklistRuleValueVO.parse(rule.ruleValue());
        } catch (IllegalArgumentException exception) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_RULE_VALUE_INVALID,
                    exception,
                    strategyId,
                    rule.ruleModel(),
                    rule.ruleValue()
            );
        }

        if (!awardIds.contains(ruleValue.fallbackAwardId())) {
            throw invalidConfig(strategyId, "strategy",
                    "blacklist fallback references unknown award " + ruleValue.fallbackAwardId()
            );
        }
    }

    /**
     * 校验奖品次数锁规则值。
     *
     * <p>次数锁允许配置为零，表示奖品没有次数门槛；负数、空值和非整数
     * 统一由 {@link LockRuleValueVO} 拒绝。</p>
     *
     * @param strategyId 策略业务标识
     * @param rule       次数锁规则配置
     * @throws BusinessException 次数锁规则值不是合法非负整数时抛出
     */
    private void validateLockRule(Long strategyId, StrategyRuleEntity rule) {
        try {
            LockRuleValueVO.parse(rule.ruleValue());
        } catch (IllegalArgumentException exception) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_RULE_VALUE_INVALID,
                    exception,
                    strategyId,
                    rule.awardId(),
                    rule.ruleValue()
            );
        }
    }

    /**
     * 校验权重档位及其候选奖品引用。
     *
     * <p>档位格式、次数门槛、重复门槛和档位内重复奖品由
     * {@link WeightRuleValueVO} 与 {@link WeightTierVO} 负责。
     * 本方法只检查每个档位引用的奖品是否属于当前策略奖池。</p>
     *
     * @param strategyId 策略业务标识
     * @param rule       权重规则配置
     * @param awardIds   当前策略奖池中的奖品标识
     * @throws BusinessException 权重规则值非法或档位引用奖池外奖品时抛出
     */
    private void validateWeightRule(Long strategyId, StrategyRuleEntity rule, Set<Long> awardIds) {
        WeightRuleValueVO ruleValue;
        try {
            ruleValue = WeightRuleValueVO.parse(rule.ruleValue());
        } catch (IllegalArgumentException exception) {
            throw BusinessException.of(BusinessResponseCode.STRATEGY_RULE_VALUE_INVALID,
                    exception,
                    strategyId,
                    rule.ruleModel(),
                    rule.ruleValue()
            );
        }

        for (WeightTierVO tier : ruleValue.tiers()) {
            if (!awardIds.containsAll(tier.awardIds())) {
                throw invalidConfig(strategyId, rule.awardId(), "weight " + tier.threshold() + " references unknown award");
            }
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

    /**
     * 策略和奖品声明的规则模型集合。
     *
     * @param strategyModels 策略级规则声明
     * @param awardModels    按奖品标识分组的奖品级规则声明
     */
    private record RuleDeclarations(Set<String> strategyModels, Map<Long, Set<String>> awardModels) {
    }

    /**
     * 已经提供参数配置的规则集合。
     *
     * @param strategyModels 已配置的策略级规则
     * @param awardModels    按奖品标识分组的已配置奖品规则
     */
    private record ConfiguredRules(Set<String> strategyModels, Map<Long, Set<String>> awardModels) {
    }
}
