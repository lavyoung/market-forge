package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.award.response.AwardConfigFieldMetadataResponse;
import com.lavyoung.marketforge.api.award.response.AwardTypeMetadataResponse;
import com.lavyoung.marketforge.application.award.model.AwardConfigFieldMetadata;
import com.lavyoung.marketforge.application.award.model.AwardTypeMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 奖品类型元数据 API 协议装配器。
 *
 * <p>负责把 Application 元数据转换为稳定 API 响应，不执行能力判断或配置校验。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AwardTypeMetadataApiAssembler {

    /**
     * 转换单个奖品类型元数据及其嵌套领取模式和表单字段。
     *
     * @param metadata Application 奖品类型元数据
     * @return API 奖品类型元数据响应
     */
    AwardTypeMetadataResponse toResponse(AwardTypeMetadata metadata);

    /**
     * 批量转换奖品类型元数据。
     *
     * @param metadataList Application 元数据列表
     * @return API 元数据响应列表
     */
    List<AwardTypeMetadataResponse> toResponses(List<AwardTypeMetadata> metadataList);

    /**
     * 将 Java 枚举字段类型转换为前端稳定的小写编码。
     *
     * @param metadata Application 配置字段元数据
     * @return API 配置字段响应
     */
    @Mapping(target = "fieldType", expression = "java(metadata.fieldType().name().toLowerCase(java.util.Locale.ROOT))")
    AwardConfigFieldMetadataResponse toResponse(AwardConfigFieldMetadata metadata);
}
