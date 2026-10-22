package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.activity.request.ActivityDefinitionSearchRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityDefinitionResponse;
import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionSearchQuery;
import com.lavyoung.marketforge.types.model.PageRequest;
import com.lavyoung.marketforge.types.model.PageResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Objects;

/**
 * 活动定义 API 协议装配器。
 *
 * <p>负责把 HTTP 查询参数转换为 Application 查询模型，并将应用结果转换为稳定的
 * API 响应。业务标识只在这一协议边界从 {@link Long} 转换为字符串。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR, imports = PageRequest.class)
public interface ActivityDefinitionApiAssembler {

    /**
     * 将 HTTP 查询请求转换为应用查询条件。
     *
     * @param request HTTP 查询请求
     * @return 应用查询条件
     */
    @Mapping(target = "page", expression = "java(new PageRequest(request.pageNumber(), request.pageSize()))")
    ActivityDefinitionSearchQuery toSearchQuery(ActivityDefinitionSearchRequest request);

    /**
     * 将活动定义应用结果转换为 API 响应。
     *
     * @param result 活动定义应用结果
     * @return 活动定义 API 响应
     */
    ActivityDefinitionResponse toResponse(ActivityDefinitionResult result);

    /**
     * 批量转换活动定义应用结果。
     *
     * @param results 活动定义应用结果列表
     * @return 活动定义 API 响应列表
     */
    List<ActivityDefinitionResponse> toResponses(List<ActivityDefinitionResult> results);

    /**
     * 转换分页记录类型并保留分页元数据。
     *
     * @param pageResult 活动定义应用分页结果
     * @return 活动定义 API 分页结果
     * @throws NullPointerException 分页结果为空时抛出
     */
    default PageResult<ActivityDefinitionResponse> toPageResponse(PageResult<ActivityDefinitionResult> pageResult) {
        Objects.requireNonNull(pageResult, "pageResult must not be null");
        return new PageResult<>(toResponses(pageResult.records()), pageResult.total(), pageResult.pageNumber(), pageResult.pageSize());
    }
}
