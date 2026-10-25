package com.lavyoung.marketforge.domain.strategy.model.aggregate;

import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.domain.strategy.model.vo.StrategyDefinitionStateVO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 策略定义聚合。
 *
 * <p>该聚合是运营配置策略时的一致性边界，同时管理 {@code strategy}、
 * {@code strategy_award} 和 {@code strategy_rule} 三类数据。Repository 保存该聚合时，
 * 必须在同一个事务内完成根配置、策略奖品和策略规则的持久化。</p>
 *
 * <p>现有 {@code StrategyEntity} 继续作为抽奖运行时读取模型。本聚合额外承担生命周期、
 * 乐观锁、审计字段以及子配置归属校验，避免高频抽奖模型被运营管理职责污染。</p>
 *
 * @param strategyId   策略业务标识
 * @param strategyDesc 策略描述
 * @param ruleModels   策略级责任链规则编码；未配置时可以为空
 * @param state        策略生命周期状态
 * @param version      乐观锁版本，从 1 开始
 * @param createdBy    创建人标识
 * @param updatedBy    最后修改人标识
 * @param createTime   创建时间；首次持久化前可以为空
 * @param updateTime   最后修改时间；首次持久化前可以为空
 * @param awards       策略拥有的奖品配置
 * @param rules        策略拥有的策略级和奖品级规则
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public record StrategyDefinitionAggregate(
        Long strategyId,
        String strategyDesc,
        String ruleModels,
        StrategyDefinitionStateVO state,
        int version,
        String createdBy,
        String updatedBy,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        List<StrategyAwardEntity> awards,
        List<StrategyRuleEntity> rules
) {

    /**
     * 创建并校验策略定义聚合。
     *
     * <p>草稿阶段允许奖品和规则集合为空，发布前的完整性校验将在后续就绪检查中完成。
     * 这里重点保证聚合标识、审计信息以及子实体归属关系正确。</p>
     *
     * @throws NullPointerException     必填字段、奖品集合或规则集合为空时抛出
     * @throws IllegalArgumentException 标识、描述、版本、操作人、子实体归属或奖品重复时抛出
     */
    public StrategyDefinitionAggregate {
        Objects.requireNonNull(strategyId, "strategyId must not be null");
        Objects.requireNonNull(state, "state must not be null");
        requireText(strategyDesc, "strategyDesc", 256);
        requireText(createdBy, "createdBy", 64);
        requireText(updatedBy, "updatedBy", 64);

        if (strategyId <= 0) {
            throw new IllegalArgumentException("strategyId must be positive");
        }
        if (ruleModels != null && ruleModels.length() > 64) {
            throw new IllegalArgumentException("ruleModels length must not exceed 64");
        }
        if (version < 1) {
            throw new IllegalArgumentException("version must be greater than zero");
        }

        awards = List.copyOf(Objects.requireNonNull(awards, "awards must not be null"));
        rules = List.copyOf(Objects.requireNonNull(rules, "rules must not be null"));

        if (awards.stream().anyMatch(award -> !strategyId.equals(award.strategyId()))) {
            throw new IllegalArgumentException("all strategy awards must belong to the aggregate strategy");
        }
        if (rules.stream().anyMatch(rule -> !strategyId.equals(rule.strategyId()))) {
            throw new IllegalArgumentException("all strategy rules must belong to the aggregate strategy");
        }
        if (awards.stream().map(StrategyAwardEntity::awardId).distinct().count() != awards.size()) {
            throw new IllegalArgumentException("strategy awards must not contain duplicate awardId");
        }
    }

    /**
     * 判断策略是否允许原地编辑。
     *
     * @return 草稿状态返回 {@code true}
     */
    public boolean canEdit() {
        return state.canEdit();
    }

    /**
     * 判断策略是否允许被活动引用。
     *
     * @return 已生效状态返回 {@code true}
     */
    public boolean canReference() {
        return state.canReference();
    }


    /**
     * 修改草稿策略的基础信息。
     *
     * <p>本方法返回新的不可变聚合，不直接增加乐观锁版本。Repository 更新时使用当前
     * {@code version} 作为数据库条件，更新成功后由数据库将版本增加一。</p>
     *
     * @param newStrategyDesc 新的策略描述
     * @param newRuleModels   新的策略级责任链规则编码
     * @param operator        执行修改的运营人员
     * @return 保留策略标识、子配置和当前乐观锁版本的新草稿聚合
     * @throws IllegalStateException    当前策略不是草稿时抛出
     * @throws NullPointerException     操作人为空时抛出
     * @throws IllegalArgumentException 描述、规则编码或操作人不合法时抛出
     */
    public StrategyDefinitionAggregate updateBasicInfo(String newStrategyDesc, String newRuleModels, String operator) {
        if (!canEdit()) {
            throw new IllegalStateException("only draft strategy can be edited");
        }

        return new StrategyDefinitionAggregate(
                strategyId, newStrategyDesc, newRuleModels,
                state, version, createdBy, operator,
                createTime, updateTime, awards, rules
        );
    }

    /**
     * 将不可编辑策略复制为新的草稿策略。
     *
     * <p>复制时使用新的策略标识，乐观锁版本重置为 1，审计时间交由持久化层重新生成。
     * 策略奖品的剩余库存重置为库存总量，因为新策略是独立配置版本，不能继承旧策略
     * 已经消耗的运行时库存。奖品和规则的业务配置保持不变，供运营人员继续调整。</p>
     *
     * @param newStrategyId 新草稿策略标识
     * @param operator      创建新草稿的运营人员
     * @return 与历史策略配置一致、但拥有独立标识和库存的新草稿聚合
     * @throws IllegalStateException    当前策略仍是可直接编辑的草稿时抛出
     * @throws NullPointerException     新策略标识或操作人为空时抛出
     * @throws IllegalArgumentException 新策略标识或操作人不合法时抛出
     */
    public StrategyDefinitionAggregate copyAsDraft(Long newStrategyId, String operator) {
        Objects.requireNonNull(newStrategyId, "newStrategyId must not be null");
        if (!state.requiresCopyForChange()) {
            throw new IllegalStateException("draft strategy should be edited directly");
        }
        if (newStrategyId <= 0 || newStrategyId.equals(strategyId)) {
            throw new IllegalArgumentException("newStrategyId must be positive and different from current strategyId");
        }
        requireText(operator, "operator", 64);
        List<StrategyAwardEntity> copiedAwards = awards.stream().map(award -> new StrategyAwardEntity(
                newStrategyId, award.awardId(), award.awardTitle(), award.awardSubtitle(),
                award.awardCount(), award.awardCount(), award.awardRate(), award.ruleModels(), award.sort(), null, null
        )).toList();
        List<StrategyRuleEntity> copiedRules = rules.stream()
                .map(rule -> new StrategyRuleEntity(
                        newStrategyId, rule.awardId(), rule.ruleType(), rule.ruleModel(),
                        rule.ruleValue(), rule.ruleDesc(), null, null
                ))
                .toList();
        return new StrategyDefinitionAggregate(newStrategyId, strategyDesc, ruleModels,
                StrategyDefinitionStateVO.DRAFT, 1, operator,
                operator, null, null, copiedAwards, copiedRules);
    }


    private static void requireText(String value, String fieldName, int maximumLength) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        if (value.length() > maximumLength) {
            throw new IllegalArgumentException(fieldName + " length must not exceed " + maximumLength);
        }
    }
}
