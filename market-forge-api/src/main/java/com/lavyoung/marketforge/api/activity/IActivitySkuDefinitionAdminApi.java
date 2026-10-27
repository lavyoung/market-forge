package com.lavyoung.marketforge.api.activity;

import com.lavyoung.marketforge.api.activity.request.ActivitySkuDefinitionSearchRequest;
import com.lavyoung.marketforge.api.activity.request.AdjustActivitySkuStockRequest;
import com.lavyoung.marketforge.api.activity.request.CreateActivitySkuDefinitionRequest;
import com.lavyoung.marketforge.api.activity.request.UpdateActivitySkuDefinitionRequest;
import com.lavyoung.marketforge.api.activity.response.ActivitySkuDefinitionResponse;
import com.lavyoung.marketforge.types.model.PageResult;
import com.lavyoung.marketforge.types.model.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 活动 SKU 运营管理 REST API 契约。
 *
 * <p>提供 SKU 分页、详情、创建、普通配置修改和库存调整能力。普通配置修改不接受
 * 库存字段，库存变化必须通过带原因和版本的独立调整接口完成。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
@Tag(name = "运营后台活动 SKU", description = "管理积分兑换活动额度商品及其库存")
@RequestMapping(path = IActivitySkuDefinitionAdminApi.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface IActivitySkuDefinitionAdminApi {

    /**
     * 活动 SKU 管理基础路径。
     */
    String BASE_PATH = "/admin/activity-skus";

    /**
     * 创建活动 SKU。
     *
     * @param request 创建请求
     * @return 创建后的 SKU 定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 活动状态、引用关系或 SKU 配置非法时抛出
     */
    @Operation(operationId = "createActivitySkuDefinition", summary = "创建活动 SKU")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<ActivitySkuDefinitionResponse> createActivitySkuDefinition(@Valid @RequestBody CreateActivitySkuDefinitionRequest request);

    /**
     * 分页查询活动 SKU。
     *
     * @param request 查询和分页条件
     * @return 活动 SKU 分页结果
     * @throws jakarta.validation.ValidationException 查询条件非法时抛出
     */
    @Operation(operationId = "searchActivitySkuDefinitions", summary = "分页查询活动 SKU")
    @GetMapping
    Response<PageResult<ActivitySkuDefinitionResponse>> searchActivitySkuDefinitions(@Valid @ModelAttribute ActivitySkuDefinitionSearchRequest request);

    /**
     * 查询活动 SKU 详情。
     *
     * @param sku SKU 业务标识
     * @return SKU 详情
     * @throws com.lavyoung.marketforge.types.exception.BusinessException SKU 不存在时抛出
     */
    @Operation(operationId = "getActivitySkuDefinition", summary = "查询活动 SKU 详情")
    @GetMapping("/{sku}")
    Response<ActivitySkuDefinitionResponse> getActivitySkuDefinition(@Positive(message = "SKU 必须为正数") @PathVariable("sku") Long sku);

    /**
     * 修改活动 SKU 普通配置。
     *
     * @param sku     SKU 业务标识
     * @param request 修改请求
     * @return 修改后的 SKU 定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 状态、归属或版本冲突时抛出
     */
    @Operation(operationId = "updateActivitySkuDefinition", summary = "修改活动 SKU 普通配置")
    @PutMapping(value = "/{sku}", consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<ActivitySkuDefinitionResponse> updateActivitySkuDefinition(@Positive(message = "SKU 必须为正数") @PathVariable("sku") Long sku, @Valid @RequestBody UpdateActivitySkuDefinitionRequest request);

    /**
     * 调整活动 SKU 库存。
     *
     * @param sku     SKU 业务标识
     * @param request 库存调整请求
     * @return 调整后的 SKU 定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 库存越界或版本冲突时抛出
     */
    @Operation(operationId = "adjustActivitySkuStock", summary = "调整活动 SKU 库存")
    @PostMapping(value = "/{sku}/stock-adjustments", consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<ActivitySkuDefinitionResponse> adjustActivitySkuStock(@Positive(message = "SKU 必须为正数") @PathVariable("sku") Long sku, @Valid @RequestBody AdjustActivitySkuStockRequest request);
}
