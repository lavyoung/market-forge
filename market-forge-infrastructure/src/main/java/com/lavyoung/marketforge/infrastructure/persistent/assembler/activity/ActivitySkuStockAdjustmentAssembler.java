package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuStockAdjustmentEntity;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivitySkuStockAdjustmentPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 活动 SKU 库存调整实体与调整流水持久化对象转换器。
 *
 * <p>该转换器只负责字段映射，不执行库存计算。库存调整前后关系已经由领域实体完成校验。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ActivitySkuStockAdjustmentAssembler {

    /**
     * 将库存调整实体转换为调整流水持久化对象。
     *
     * @param entity 库存调整实体
     * @return 库存调整流水持久化对象
     */
    @Mapping(target = "id", ignore = true)
    ActivitySkuStockAdjustmentPO toPO(ActivitySkuStockAdjustmentEntity entity);

    /**
     * 将库存调整流水持久化对象转换为领域实体。
     *
     * @param po 库存调整流水持久化对象
     * @return 库存调整实体
     */
    ActivitySkuStockAdjustmentEntity toEntity(ActivitySkuStockAdjustmentPO po);
}
