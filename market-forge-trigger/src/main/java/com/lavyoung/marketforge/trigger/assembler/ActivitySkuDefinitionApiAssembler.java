package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.activity.request.ActivitySkuDefinitionSearchRequest;
import com.lavyoung.marketforge.api.activity.request.AdjustActivitySkuStockRequest;
import com.lavyoung.marketforge.api.activity.request.CreateActivitySkuDefinitionRequest;
import com.lavyoung.marketforge.api.activity.request.UpdateActivitySkuDefinitionRequest;
import com.lavyoung.marketforge.api.activity.response.ActivitySkuDefinitionResponse;
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
 * 活动 SKU API 协议装配器。
 *
 * <p>负责 API 与 Application 模型转换，并在 HTTP 边界完成十八位业务标识的
 * 字符串与 {@link Long} 转换。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ActivitySkuDefinitionApiAssembler {

    /**
     * 将分页请求转换为应用查询条件。
     *
     * @param request HTTP 查询请求
     * @return 应用查询条件
     */
    default ActivitySkuDefinitionSearchQuery toSearchQuery(ActivitySkuDefinitionSearchRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return new ActivitySkuDefinitionSearchQuery(parseNullableId(request.sku()),
                parseNullableId(request.activityId()), parseNullableId(request.activityCountId()),
                new PageRequest(request.pageNumber(), request.pageSize()));
    }

    /**
     * 将创建请求和认证操作人组合为应用命令。
     *
     * @param request  创建请求
     * @param operator 当前认证操作人
     * @return 创建命令
     */
    default CreateActivitySkuDefinitionCommand toCreateCommand(CreateActivitySkuDefinitionRequest request, String operator) {
        Objects.requireNonNull(request, "request must not be null");
        return new CreateActivitySkuDefinitionCommand(Long.valueOf(request.activityId()),
                Long.valueOf(request.activityCountId()), request.stockCount(), request.creditAmount(), operator);
    }

    /**
     * 将路径 SKU、修改请求和认证操作人组合为应用命令。
     *
     * @param sku      路径中的 SKU
     * @param request  修改请求
     * @param operator 当前认证操作人
     * @return 修改命令
     */
    default UpdateActivitySkuDefinitionCommand toUpdateCommand(Long sku, UpdateActivitySkuDefinitionRequest request, String operator) {
        Objects.requireNonNull(request, "request must not be null");
        return new UpdateActivitySkuDefinitionCommand(sku, Long.valueOf(request.activityId()),
                Long.valueOf(request.activityCountId()), request.creditAmount(), request.expectedVersion(), operator);
    }

    /**
     * 将路径 SKU、库存调整请求和认证操作人组合为应用命令。
     *
     * @param sku      路径中的 SKU
     * @param request  库存调整请求
     * @param operator 当前认证操作人
     * @return 库存调整命令
     */
    default AdjustActivitySkuStockCommand toAdjustStockCommand(Long sku, AdjustActivitySkuStockRequest request, String operator) {
        Objects.requireNonNull(request, "request must not be null");
        return new AdjustActivitySkuStockCommand(sku, request.quantityDelta(), request.reason(),
                request.expectedVersion(), operator);
    }

    /**
     * 将应用结果转换为 API 响应。
     *
     * @param result 应用结果
     * @return API 响应
     */
    @Mapping(target = "sku", expression = "java(String.valueOf(result.sku()))")
    @Mapping(target = "activityId", expression = "java(String.valueOf(result.activityId()))")
    @Mapping(target = "activityCountId", expression = "java(String.valueOf(result.activityCountId()))")
    @Mapping(target = "supportsCreditExchange", expression = "java(result.creditAmount() != null)")
    ActivitySkuDefinitionResponse toResponse(ActivitySkuDefinitionResult result);

    /**
     * 批量转换应用结果。
     *
     * @param results 应用结果列表
     * @return API 响应列表
     */
    List<ActivitySkuDefinitionResponse> toResponses(List<ActivitySkuDefinitionResult> results);

    /**
     * 转换分页结果并保留分页元数据。
     *
     * @param pageResult 应用分页结果
     * @return API 分页响应
     */
    default PageResult<ActivitySkuDefinitionResponse> toPageResponse(PageResult<ActivitySkuDefinitionResult> pageResult) {
        Objects.requireNonNull(pageResult, "pageResult must not be null");
        return new PageResult<>(toResponses(pageResult.records()), pageResult.total(),
                pageResult.pageNumber(), pageResult.pageSize());
    }

    private static Long parseNullableId(String value) {
        return value == null || value.isBlank() ? null : Long.valueOf(value);
    }
}
