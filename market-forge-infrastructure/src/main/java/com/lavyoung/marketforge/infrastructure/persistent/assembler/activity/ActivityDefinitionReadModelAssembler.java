package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionResult;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityPO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 活动定义运营读模型装配器。
 *
 * <p>后台分页查询不执行活动领域规则，因此直接把 {@link ActivityPO} 投影为
 * Application 查询结果，避免为只读列表无意义地装载领域实体。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ActivityDefinitionReadModelAssembler {

    /**
     * 将活动持久化对象转换为应用查询结果。
     *
     * @param source 活动持久化对象
     * @return 活动定义应用结果
     */
    ActivityDefinitionResult toResult(ActivityPO source);

    /**
     * 批量转换活动定义分页记录。
     *
     * @param sources 活动持久化对象列表
     * @return 活动定义应用结果列表
     */
    List<ActivityDefinitionResult> toResults(List<ActivityPO> sources);
}
