package com.lavyoung.marketforge.infrastructure.persistent.mapper;

import com.lavyoung.marketforge.domain.strategy.model.StrategyEntity;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyPO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 策略奖品持久化对象与领域实体的转换器。
 *
 * @author lavyoung
 * @version 1.0.0-SNAPSHOT
 * @email lavyoung1325@outlook.com
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface StrategyMapper {

    /**
     * 将策略奖品持久化对象转换为领域实体。
     *
     * @param strategyAward 策略奖品持久化对象
     * @return 策略奖品领域实体
     */
    StrategyEntity toEntity(StrategyPO strategyAward);

    /**
     * 批量将策略奖品持久化对象转换为领域实体。
     *
     * @param strategyAwards 策略奖品持久化对象列表
     * @return 策略奖品领域实体列表
     */
    List<StrategyEntity> toEntities(List<StrategyPO> strategyAwards);

}
