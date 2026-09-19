package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityAccountFlowEntity;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityAccountFlowPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 用户活动账户流水持久化对象与领域实体转换器。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ActivityAccountFlowAssembler {

    /**
     * 将活动账户流水持久化对象转换为领域实体。
     *
     * @param activityAccountFlowPO 活动账户流水持久化对象
     * @return 活动账户流水领域实体
     */
    ActivityAccountFlowEntity toEntity(ActivityAccountFlowPO activityAccountFlowPO);

    /**
     * 批量将活动账户流水持久化对象转换为领域实体。
     *
     * @param activityAccountFlowPOs 活动账户流水持久化对象列表
     * @return 活动账户流水领域实体列表
     */
    List<ActivityAccountFlowEntity> toEntities(List<ActivityAccountFlowPO> activityAccountFlowPOs);

    /**
     * 将活动账户流水领域实体转换为待持久化对象。
     *
     * @param activityAccountFlowEntity 活动账户流水领域实体
     * @return 活动账户流水持久化对象
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    ActivityAccountFlowPO toPO(ActivityAccountFlowEntity activityAccountFlowEntity);

    /**
     * 批量将活动账户流水领域实体转换为待持久化对象。
     *
     * @param activityAccountFlowEntities 活动账户流水领域实体列表
     * @return 活动账户流水持久化对象列表
     */
    List<ActivityAccountFlowPO> toPOs(List<ActivityAccountFlowEntity> activityAccountFlowEntities);
}
