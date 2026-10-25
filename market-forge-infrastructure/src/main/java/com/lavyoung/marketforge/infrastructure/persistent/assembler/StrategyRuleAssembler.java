package com.lavyoung.marketforge.infrastructure.persistent.assembler;

import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyRulePO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 策略规则持久化对象与领域实体的转换器。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface StrategyRuleAssembler {

    /**
     * 将策略规则持久化对象转换为领域实体。
     *
     * @param strategyAward 策略规则持久化对象
     * @return 策略规则领域实体；输入为 {@code null} 时返回 {@code null}
     */
    StrategyRuleEntity toEntity(StrategyRulePO strategyAward);

    /**
     * 批量将策略规则持久化对象转换为领域实体。
     *
     * @param strategyAwards 策略规则持久化对象列表
     * @return 策略规则领域实体列表；输入为 {@code null} 时返回 {@code null}
     */
    List<StrategyRuleEntity> toEntities(List<StrategyRulePO> strategyAwards);

    /**
     * 将策略规则领域实体转换为持久化对象。
     *
     * <p>数据库自增主键由数据库生成，因此转换时忽略 {@code id}。</p>
     *
     * @param strategyRule 策略规则领域实体
     * @return 可供 MyBatis 批量写入的持久化对象
     */
    @Mapping(target = "id", ignore = true)
    StrategyRulePO toPO(StrategyRuleEntity strategyRule);

    /**
     * 批量将策略规则领域实体转换为持久化对象。
     *
     * @param strategyRules 策略规则领域实体列表
     * @return 策略规则持久化对象列表
     */
    List<StrategyRulePO> toPOs(List<StrategyRuleEntity> strategyRules);
}
