package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.activity.request.ActivityCountDefinitionSearchRequest;
import com.lavyoung.marketforge.api.activity.request.CreateActivityCountDefinitionRequest;
import com.lavyoung.marketforge.api.activity.request.UpdateActivityCountDefinitionRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityCountDefinitionResponse;
import com.lavyoung.marketforge.application.activity.model.ActivityCountDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ActivityCountDefinitionSearchQuery;
import com.lavyoung.marketforge.application.activity.model.CreateActivityCountDefinitionCommand;
import com.lavyoung.marketforge.application.activity.model.UpdateActivityCountDefinitionCommand;
import com.lavyoung.marketforge.types.model.PageRequest;
import com.lavyoung.marketforge.types.model.PageResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Objects;

/**
 * 活动次数配置 API 协议装配器。
 *
 * <p>负责 API 与 Application 模型转换，并只在 HTTP 边界转换长整型业务标识。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ActivityCountDefinitionApiAssembler {

    /**
     * 将查询请求转换为应用查询条件。
     *
     * @param request HTTP 查询请求
     * @return 应用查询条件
     */
    default ActivityCountDefinitionSearchQuery toSearchQuery(ActivityCountDefinitionSearchRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        Long activityCountId = request.activityCountId() == null || request.activityCountId().isBlank()
                ? null : Long.valueOf(request.activityCountId());
        return new ActivityCountDefinitionSearchQuery(activityCountId, request.activityCountName(),
                new PageRequest(request.pageNumber(), request.pageSize()));
    }

    /**
     * 将创建请求和认证操作人组合为应用命令。
     *
     * @param request  创建请求
     * @param operator 当前认证操作人
     * @return 创建命令
     */
    @Mapping(target = "operator", source = "operator")
    CreateActivityCountDefinitionCommand toCreateCommand(CreateActivityCountDefinitionRequest request, String operator);

    /**
     * 将路径标识、修改请求和认证操作人组合为应用命令。
     *
     * @param activityCountId 路径中的配置标识
     * @param request         修改请求
     * @param operator        当前认证操作人
     * @return 修改命令
     */
    @Mapping(target = "activityCountId", source = "activityCountId")
    @Mapping(target = "operator", source = "operator")
    UpdateActivityCountDefinitionCommand toUpdateCommand(Long activityCountId, UpdateActivityCountDefinitionRequest request, String operator);

    /**
     * 将应用结果转换为 API 响应。
     *
     * @param result 应用结果
     * @return API 响应
     */
    @Mapping(target = "activityCountId", expression = "java(String.valueOf(result.activityCountId()))")
    ActivityCountDefinitionResponse toResponse(ActivityCountDefinitionResult result);

    /**
     * 批量转换应用结果。
     *
     * @param results 应用结果列表
     * @return API 响应列表
     */
    List<ActivityCountDefinitionResponse> toResponses(List<ActivityCountDefinitionResult> results);

    /**
     * 转换分页结果并保留分页元数据。
     *
     * @param pageResult 应用分页结果
     * @return API 分页响应
     */
    default PageResult<ActivityCountDefinitionResponse> toPageResponse(PageResult<ActivityCountDefinitionResult> pageResult) {
        Objects.requireNonNull(pageResult, "pageResult must not be null");
        return new PageResult<>(toResponses(pageResult.records()), pageResult.total(),
                pageResult.pageNumber(), pageResult.pageSize());
    }
}
