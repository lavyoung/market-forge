package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountEntity;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityCountPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 活动参与次数配置持久化对象与领域实体转换器。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ActivityCountAssembler {

    /**
     * 将次数配置持久化对象转换为领域实体。
     *
     * @param activityCountPO 次数配置持久化对象
     * @return 次数配置领域实体
     */
    ActivityCountEntity toEntity(ActivityCountPO activityCountPO);

    /**
     * 批量将次数配置持久化对象转换为领域实体。
     *
     * @param activityCountPOs 次数配置持久化对象列表
     * @return 次数配置领域实体列表
     */
    List<ActivityCountEntity> toEntities(List<ActivityCountPO> activityCountPOs);

    /**
     * 将次数配置领域实体转换为待持久化对象。
     *
     * @param activityCountEntity 次数配置领域实体
     * @return 次数配置持久化对象
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    ActivityCountPO toPO(ActivityCountEntity activityCountEntity);

    /**
     * 批量将次数配置领域实体转换为待持久化对象。
     *
     * @param activityCountEntities 次数配置领域实体列表
     * @return 次数配置持久化对象列表
     */
    List<ActivityCountPO> toPOs(List<ActivityCountEntity> activityCountEntities);
}
