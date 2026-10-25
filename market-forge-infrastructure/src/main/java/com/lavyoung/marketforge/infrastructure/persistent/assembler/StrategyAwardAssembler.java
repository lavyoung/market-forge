package com.lavyoung.marketforge.infrastructure.persistent.assembler;

import com.lavyoung.marketforge.domain.strategy.model.entity.StrategyAwardEntity;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyAwardPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 策略奖品持久化对象与领域实体的转换器。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0-SNAPSHOT
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface StrategyAwardAssembler {

    /**
     * 将策略奖品持久化对象转换为领域实体。
     *
     * @param strategyAward 策略奖品持久化对象
     * @return 策略奖品领域实体
     */
    StrategyAwardEntity toEntity(StrategyAwardPO strategyAward);

    /**
     * 批量将策略奖品持久化对象转换为领域实体。
     *
     * @param strategyAwards 策略奖品持久化对象列表
     * @return 策略奖品领域实体列表
     */
    List<StrategyAwardEntity> toEntities(List<StrategyAwardPO> strategyAwards);

    /**
     * 将策略奖品领域实体转换为持久化对象。
     *
     * <p>数据库自增主键由数据库生成，因此转换时忽略 {@code id}。</p>
     *
     * @param strategyAward 策略奖品领域实体
     * @return 可供 MyBatis 批量写入的持久化对象
     */
    @Mapping(target = "id", ignore = true)
    StrategyAwardPO toPO(StrategyAwardEntity strategyAward);

    /**
     * 批量将策略奖品领域实体转换为持久化对象。
     *
     * @param strategyAwards 策略奖品领域实体列表
     * @return 策略奖品持久化对象列表
     */
    List<StrategyAwardPO> toPOs(List<StrategyAwardEntity> strategyAwards);
}
