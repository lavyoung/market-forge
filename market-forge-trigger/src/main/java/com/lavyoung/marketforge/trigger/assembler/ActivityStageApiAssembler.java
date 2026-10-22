package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.activity.request.ActivityStageSearchRequest;
import com.lavyoung.marketforge.api.activity.request.CreateActivityStageRequest;
import com.lavyoung.marketforge.api.activity.request.PublishActivityStageRequest;
import com.lavyoung.marketforge.api.activity.request.UpdateActivityStageRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityStageResponse;
import com.lavyoung.marketforge.application.activity.model.*;
import com.lavyoung.marketforge.types.model.PageRequest;
import com.lavyoung.marketforge.types.model.PageResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Objects;

/**
 * 活动展台 API 协议装配器。
 *
 * <p>负责 API Request、Application Command、Application Result 和 API Response
 * 之间的转换。活动与展台编号只在协议边界进行字符串和长整型转换。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR, imports = PageRequest.class)
public interface ActivityStageApiAssembler {

    /**
     * 将 HTTP 查询请求转换为应用查询条件。
     *
     * @param request HTTP 查询请求
     * @return 应用查询条件
     */
    @Mapping(target = "page", expression = "java(new PageRequest(request.pageNumber(), request.pageSize()))")
    ActivityStageSearchQuery toSearchQuery(ActivityStageSearchRequest request);

    /**
     * 将创建请求和认证操作人组合为应用命令。
     *
     * @param request  创建请求
     * @param operator 当前认证操作人
     * @return 创建命令
     */
    @Mapping(target = "activityId", expression = "java(Long.valueOf(request.activityId()))")
    @Mapping(target = "operator", source = "operator")
    CreateActivityStageCommand toCreateCommand(CreateActivityStageRequest request, String operator);

    /**
     * 将路径标识、修改请求和认证操作人组合为应用命令。
     *
     * @param stageId  展台路径标识
     * @param request  修改请求
     * @param operator 当前认证操作人
     * @return 修改命令
     */
    @Mapping(target = "stageId", source = "stageId")
    @Mapping(target = "activityId", expression = "java(Long.valueOf(request.activityId()))")
    @Mapping(target = "operator", source = "operator")
    UpdateActivityStageCommand toUpdateCommand(Long stageId, UpdateActivityStageRequest request, String operator);

    /**
     * 将路径标识、发布版本和认证操作人组合为发布命令。
     *
     * @param stageId  展台路径标识
     * @param request  发布请求
     * @param operator 当前认证操作人
     * @return 发布命令
     */
    @Mapping(target = "stageId", source = "stageId")
    @Mapping(target = "expectedVersion", source = "request.expectedVersion")
    @Mapping(target = "operator", source = "operator")
    PublishActivityStageCommand toPublishCommand(Long stageId, PublishActivityStageRequest request, String operator);

    /**
     * 将应用结果转换为 API 响应。
     *
     * @param result 活动展台应用结果
     * @return 活动展台 API 响应
     */
    ActivityStageResponse toResponse(ActivityStageResult result);

    /**
     * 批量转换活动展台应用结果。
     *
     * @param results 应用结果列表
     * @return API 响应列表
     */
    List<ActivityStageResponse> toResponses(List<ActivityStageResult> results);

    /**
     * 转换分页记录类型并保留分页元数据。
     *
     * @param pageResult 活动展台应用分页结果
     * @return 活动展台 API 分页结果
     * @throws NullPointerException 分页结果为空时抛出
     */
    default PageResult<ActivityStageResponse> toPageResponse(PageResult<ActivityStageResult> pageResult) {
        Objects.requireNonNull(pageResult, "pageResult must not be null");
        return new PageResult<>(toResponses(pageResult.records()), pageResult.total(), pageResult.pageNumber(), pageResult.pageSize());
    }
}
