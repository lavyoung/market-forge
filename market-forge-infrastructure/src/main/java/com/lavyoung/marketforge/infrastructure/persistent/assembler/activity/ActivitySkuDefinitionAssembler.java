package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuDefinitionEntity;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivitySkuPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 活动 SKU 运营定义与持久化对象转换器。
 *
 * <p>负责在运营写模型和数据库模型之间转换，不参与运行时库存扣减或额度入账。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ActivitySkuDefinitionAssembler {

    /**
     * 将持久化对象转换为 SKU 运营定义。
     *
     * @param po SKU 持久化对象
     * @return SKU 运营定义
     */
    ActivitySkuDefinitionEntity toEntity(ActivitySkuPO po);

    /**
     * 将 SKU 运营定义转换为持久化对象。
     *
     * <p>数据库主键由数据库生成，因此转换时忽略 {@code id}。</p>
     *
     * @param entity SKU 运营定义
     * @return SKU 持久化对象
     */
    @Mapping(target = "id", ignore = true)
    ActivitySkuPO toPO(ActivitySkuDefinitionEntity entity);
}
