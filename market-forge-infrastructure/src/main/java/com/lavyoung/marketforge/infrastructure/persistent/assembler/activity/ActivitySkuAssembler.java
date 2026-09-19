package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivitySkuEntity;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivitySkuPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 抽奖活动 SKU 持久化对象与领域实体转换器。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/20
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ActivitySkuAssembler {

    /**
     * 将活动 SKU 持久化对象转换为领域实体。
     *
     * @param activitySkuPO 活动 SKU 持久化对象
     * @return 活动 SKU 领域实体
     */
    ActivitySkuEntity toEntity(ActivitySkuPO activitySkuPO);

    /**
     * 批量将活动 SKU 持久化对象转换为领域实体。
     *
     * @param activitySkuPOs 活动 SKU 持久化对象列表
     * @return 活动 SKU 领域实体列表
     */
    List<ActivitySkuEntity> toEntities(List<ActivitySkuPO> activitySkuPOs);

    /**
     * 将活动 SKU 领域实体转换为待持久化对象。
     *
     * @param activitySkuEntity 活动 SKU 领域实体
     * @return 活动 SKU 持久化对象
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    ActivitySkuPO toPO(ActivitySkuEntity activitySkuEntity);

    /**
     * 批量将活动 SKU 领域实体转换为待持久化对象。
     *
     * @param activitySkuEntities 活动 SKU 领域实体列表
     * @return 活动 SKU 持久化对象列表
     */
    List<ActivitySkuPO> toPOs(List<ActivitySkuEntity> activitySkuEntities);
}
