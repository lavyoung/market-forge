package com.lavyoung.marketforge.infrastructure.persistent.assembler;

import com.lavyoung.marketforge.domain.strategy.model.entity.AwardEntity;
import com.lavyoung.marketforge.infrastructure.persistent.po.AwardPO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 奖品基础配置持久化转换器。
 * <p>
 * 负责在 {@link AwardPO} 和领域层 {@link AwardEntity} 之间转换，
 * 防止应用层或领域层直接依赖 MyBatis 持久化对象。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AwardAssembler {

    /**
     * 将奖品持久化对象转换为领域实体。
     *
     * @param awardPO 奖品持久化对象
     * @return 奖品领域实体
     */
    AwardEntity toEntity(AwardPO awardPO);
}
