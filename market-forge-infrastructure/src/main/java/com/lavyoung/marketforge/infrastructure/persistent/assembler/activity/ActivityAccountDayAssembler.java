package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityAccountDayEntity;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityAccountDayPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 用户活动日账户持久化对象与领域实体转换器。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ActivityAccountDayAssembler {

    /**
     * 将用户活动日账户持久化对象转换为领域实体。
     *
     * @param source 用户活动日账户持久化对象
     * @return 用户活动日账户领域实体
     */
    ActivityAccountDayEntity toEntity(ActivityAccountDayPO source);

    /**
     * 批量将用户活动日账户持久化对象转换为领域实体。
     *
     * @param sources 用户活动日账户持久化对象列表
     * @return 用户活动日账户领域实体列表
     */
    List<ActivityAccountDayEntity> toEntities(List<ActivityAccountDayPO> sources);

    /**
     * 将用户活动日账户领域实体转换为待新增的持久化对象。
     *
     * @param source 用户活动日账户领域实体
     * @return 忽略数据库生成字段后的用户活动日账户持久化对象
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    ActivityAccountDayPO toPO(ActivityAccountDayEntity source);

    /**
     * 批量将用户活动日账户领域实体转换为待新增的持久化对象。
     *
     * @param sources 用户活动日账户领域实体列表
     * @return 忽略数据库生成字段后的用户活动日账户持久化对象列表
     */
    List<ActivityAccountDayPO> toPOs(List<ActivityAccountDayEntity> sources);
}
