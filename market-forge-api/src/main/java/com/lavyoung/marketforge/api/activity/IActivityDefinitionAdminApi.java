package com.lavyoung.marketforge.api.activity;

import com.lavyoung.marketforge.api.activity.request.ActivityDefinitionSearchRequest;
import com.lavyoung.marketforge.api.activity.request.ChangeActivityDefinitionStateRequest;
import com.lavyoung.marketforge.api.activity.request.CreateActivityDefinitionRequest;
import com.lavyoung.marketforge.api.activity.request.UpdateActivityDefinitionRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityDefinitionResponse;
import com.lavyoung.marketforge.types.model.PageResult;
import com.lavyoung.marketforge.types.model.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 活动定义运营查询 REST API 契约。
 *
 * <p>该接口查询 {@code raffle_activity} 中的活动定义，与负责渠道发布记录的
 * {@link IActivityStageAdminApi} 分离。活动定义管理基础配置和生命周期，活动展台负责预热与渠道发布。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Tag(name = "运营后台活动管理", description = "提供活动定义创建、查询、编辑和生命周期管理能力")
@RequestMapping(path = IActivityDefinitionAdminApi.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface IActivityDefinitionAdminApi {

    /**
     * 运营后台活动定义基础路径。
     */
    String BASE_PATH = "/admin/activities";

    /**
     * 创建活动草稿。
     *
     * @param request 创建请求
     * @return 创建后的活动定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 活动配置非法或活动标识冲突时抛出
     */
    @Operation(operationId = "createActivityDefinition", summary = "创建活动定义")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<ActivityDefinitionResponse> createActivityDefinition(@Valid @RequestBody CreateActivityDefinitionRequest request);

    /**
     * 分页查询活动定义。
     *
     * <p>活动标识使用精确匹配，活动名称使用前缀匹配，结果按照更新时间和数据库
     * 主键倒序稳定排列。该操作不会修改活动状态或触发预热发布。</p>
     *
     * @param request 活动定义过滤条件和分页参数
     * @return 满足条件的活动定义分页结果
     * @throws jakarta.validation.ValidationException                     查询条件或分页参数不合法时抛出
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 数据库查询失败时抛出
     */
    @Operation(operationId = "searchActivityDefinitions", summary = "分页查询活动定义")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "查询条件或分页参数非法"),
            @ApiResponse(responseCode = "401", description = "运营人员尚未登录"),
            @ApiResponse(responseCode = "403", description = "没有查询权限")
    })
    @GetMapping
    Response<PageResult<ActivityDefinitionResponse>> searchActivityDefinitions(@Valid @ModelAttribute ActivityDefinitionSearchRequest request);

    /**
     * 查询活动定义详情。
     *
     * @param activityId 活动标识
     * @return 活动定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 活动不存在时抛出
     */
    @Operation(operationId = "getActivityDefinition", summary = "查询活动定义详情")
    @GetMapping("/{activityId}")
    Response<ActivityDefinitionResponse> getActivityDefinition(@Positive(message = "活动标识必须为正数") @PathVariable Long activityId);

    /**
     * 修改活动草稿。
     *
     * @param activityId 活动标识
     * @param request    修改请求
     * @return 修改后的活动定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 活动不存在、状态不允许编辑或版本冲突时抛出
     */
    @Operation(operationId = "updateActivityDefinition", summary = "修改活动草稿")
    @PutMapping(value = "/{activityId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<ActivityDefinitionResponse> updateActivityDefinition(@Positive(message = "活动标识必须为正数") @PathVariable Long activityId, @Valid @RequestBody UpdateActivityDefinitionRequest request);

    /**
     * 推进活动生命周期。
     *
     * @param activityId 活动标识
     * @param request    状态变更请求
     * @return 状态变更后的活动定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 活动不存在、生命周期迁移非法或版本冲突时抛出
     */
    @Operation(operationId = "changeActivityDefinitionState", summary = "变更活动定义状态")
    @PatchMapping(value = "/{activityId}/state", consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<ActivityDefinitionResponse> changeActivityDefinitionState(@Positive(message = "活动标识必须为正数") @PathVariable Long activityId, @Valid @RequestBody ChangeActivityDefinitionStateRequest request);
}
