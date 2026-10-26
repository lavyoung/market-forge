package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.application.activity.model.ActivityCountDefinitionResult;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityCountPO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 活动次数配置运营读模型装配器。
 *
 * <p>分页查询不执行领域行为，因此直接从持久化对象转换为 Application Result。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ActivityCountDefinitionReadModelAssembler {

    /**
     * 批量转换活动次数配置分页记录。
     *
     * @param sources 持久化对象列表
     * @return 应用结果列表
     */
    List<ActivityCountDefinitionResult> toResults(List<ActivityCountPO> sources);
}
