package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountDefinitionEntity;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityCountPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 活动次数配置运营定义持久化装配器。
 *
 * <p>与只转换运行时三个额度字段的 {@link ActivityCountAssembler} 分离，完整保留配置名称、
 * 乐观锁版本和审计信息。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ActivityCountDefinitionAssembler {

    /**
     * 将持久化对象转换为运营领域定义。
     *
     * @param source 活动次数配置持久化对象
     * @return 活动次数配置运营定义
     */
    ActivityCountDefinitionEntity toEntity(ActivityCountPO source);

    /**
     * 将运营领域定义转换为持久化对象。
     *
     * @param source 活动次数配置运营定义
     * @return 待持久化对象
     */
    @Mapping(target = "id", ignore = true)
    ActivityCountPO toPO(ActivityCountDefinitionEntity source);
}
