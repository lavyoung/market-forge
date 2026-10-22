package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.application.activity.model.ActivityStageResult;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityStagePO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 活动展台运营读模型装配器。
 *
 * <p>分页列表无需构造领域实体或执行生命周期规则，因此直接把持久化对象转换为
 * Application Result，减少只读路径的无意义领域装载。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ActivityStageReadModelAssembler {

    /**
     * 批量转换活动展台分页记录。
     *
     * @param sources 活动展台持久化对象
     * @return 活动展台应用结果
     */
    List<ActivityStageResult> toResults(List<ActivityStagePO> sources);
}
