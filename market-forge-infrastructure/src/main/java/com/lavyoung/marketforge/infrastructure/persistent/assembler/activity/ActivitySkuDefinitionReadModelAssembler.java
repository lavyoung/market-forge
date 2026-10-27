package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.application.activity.model.ActivitySkuDefinitionResult;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivitySkuPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 活动 SKU 运营读模型装配器。
 *
 * <p>分页查询不执行领域行为，因此直接将持久化投影转换为 Application Result。
 * 已消费库存由总库存减去剩余库存得到，不在数据库中冗余保存。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ActivitySkuDefinitionReadModelAssembler {

    /**
     * 转换单条活动 SKU 查询记录。
     *
     * @param source 持久化对象
     * @return 应用查询结果
     */
    @Mapping(target = "consumedCount", expression = "java(source.getStockCount() - source.getStockCountSurplus())")
    ActivitySkuDefinitionResult toResult(ActivitySkuPO source);

    /**
     * 批量转换活动 SKU 查询记录。
     *
     * @param sources 持久化对象列表
     * @return 应用查询结果列表
     */
    List<ActivitySkuDefinitionResult> toResults(List<ActivitySkuPO> sources);
}
