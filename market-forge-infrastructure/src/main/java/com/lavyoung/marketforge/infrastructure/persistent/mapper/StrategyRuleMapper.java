package com.lavyoung.marketforge.infrastructure.persistent.mapper;

import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyRuleEntity;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyRulePO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 策略规则持久化对象与领域实体的转换器。
 *
 * @author lavyoung
 * @version 1.0.0-SNAPSHOT
 * @email lavyoung1325@outlook.com
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface StrategyRuleMapper {

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

}
