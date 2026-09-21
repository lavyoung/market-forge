package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityAccountEntity;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityAccountPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 用户活动账户持久化对象与领域实体转换器。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ActivityAccountAssembler {

    /**
     * 将活动账户持久化对象转换为领域实体。
     *
     * @param activityAccountPO 活动账户持久化对象
     * @return 活动账户领域实体
     */
    ActivityAccountEntity toEntity(ActivityAccountPO activityAccountPO);

    /**
     * 批量将活动账户持久化对象转换为领域实体。
     *
     * @param activityAccountPOs 活动账户持久化对象列表
     * @return 活动账户领域实体列表
     */
    List<ActivityAccountEntity> toEntities(List<ActivityAccountPO> activityAccountPOs);

    /**
     * 将活动账户领域实体转换为待持久化对象。
     *
     * @param activityAccountEntity 活动账户领域实体
     * @return 活动账户持久化对象
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "version", ignore = true)
    ActivityAccountPO toPO(ActivityAccountEntity activityAccountEntity);

    /**
     * 批量将活动账户领域实体转换为待持久化对象。
     *
     * @param activityAccountEntities 活动账户领域实体列表
     * @return 活动账户持久化对象列表
     */
    List<ActivityAccountPO> toPOs(List<ActivityAccountEntity> activityAccountEntities);
}
