package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityOrderEntity;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityOrderPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 抽奖活动订单持久化对象与领域实体转换器。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ActivityOrderAssembler {

    /**
     * 将活动订单持久化对象转换为领域实体。
     *
     * @param activityOrderPO 活动订单持久化对象
     * @return 活动订单领域实体
     */
    ActivityOrderEntity toEntity(ActivityOrderPO activityOrderPO);

    /**
     * 批量将活动订单持久化对象转换为领域实体。
     *
     * @param activityOrderPOs 活动订单持久化对象列表
     * @return 活动订单领域实体列表
     */
    List<ActivityOrderEntity> toEntities(List<ActivityOrderPO> activityOrderPOs);

    /**
     * 将活动订单领域实体转换为待持久化对象。
     *
     * @param activityOrderEntity 活动订单领域实体
     * @return 活动订单持久化对象
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    ActivityOrderPO toPO(ActivityOrderEntity activityOrderEntity);

    /**
     * 批量将活动订单领域实体转换为待持久化对象。
     *
     * @param activityOrderEntities 活动订单领域实体列表
     * @return 活动订单持久化对象列表
     */
    List<ActivityOrderPO> toPOs(List<ActivityOrderEntity> activityOrderEntities);
}
