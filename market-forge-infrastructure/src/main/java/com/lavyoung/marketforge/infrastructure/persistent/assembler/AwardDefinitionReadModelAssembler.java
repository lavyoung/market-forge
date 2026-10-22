package com.lavyoung.marketforge.infrastructure.persistent.assembler;

import com.lavyoung.marketforge.application.award.model.AwardDefinitionResult;
import com.lavyoung.marketforge.infrastructure.persistent.po.AwardPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 奖品定义运营读模型装配器。
 *
 * <p>负责把 MyBatis 持久化对象转换为 Application 查询结果。
 * 该装配器仅服务只读运营查询，不参与奖品生命周期判断或持久化写入。</p>
 *
 * <p>数据库字段 {@code award_key} 在持久化对象中表达为 {@code awardKey}，
 * 在应用模型中统一表达为 {@code awardCode}。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AwardDefinitionReadModelAssembler {

    /**
     * 将奖品持久化对象转换为应用查询结果。
     *
     * @param source 奖品持久化对象
     * @return 奖品定义应用结果
     */
    @Mapping(target = "awardCode", source = "awardKey")
    AwardDefinitionResult toResult(AwardPO source);

    /**
     * 批量转换奖品定义查询结果。
     *
     * @param sources 奖品持久化对象列表
     * @return 奖品定义应用结果列表
     */
    List<AwardDefinitionResult> toResults(List<AwardPO> sources);
}