package com.lavyoung.marketforge.api.activity;

import com.lavyoung.marketforge.api.activity.request.ActivityStageSearchRequest;
import com.lavyoung.marketforge.api.activity.request.CreateActivityStageRequest;
import com.lavyoung.marketforge.api.activity.request.PublishActivityStageRequest;
import com.lavyoung.marketforge.api.activity.request.UpdateActivityStageRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityStageResponse;
import com.lavyoung.marketforge.types.model.PageResult;
import com.lavyoung.marketforge.types.model.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 活动展台运营管理 REST API 契约。
 *
 * <p>展台负责把渠道与来源映射到一个可参与活动。创建和编辑只维护草稿，发布操作会先
 * 预热活动运行时数据，再原子替换同渠道来源下原有的生效记录。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Tag(name = "运营后台活动上架", description = "提供活动展台创建、查询、编辑和发布能力")
@RequestMapping(path = IActivityStageAdminApi.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface IActivityStageAdminApi {

    /**
     * 运营后台活动展台基础路径。
     */
    String BASE_PATH = "/admin/activity-stages";

    /**
     * 创建待上架活动展台。
     *
     * @param request 创建请求
     * @return 创建后的展台配置
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 配置重复、参数非法或写入失败时抛出
     */
    @Operation(operationId = "createActivityStage", summary = "创建活动展台")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<ActivityStageResponse> createActivityStage(@Valid @RequestBody CreateActivityStageRequest request);

    /**
     * 分页查询活动展台。
     *
     * @param request 查询和分页条件
     * @return 活动展台分页结果
     * @throws jakarta.validation.ValidationException 查询条件非法时抛出
     */
    @Operation(operationId = "searchActivityStages", summary = "分页查询活动展台")
    @GetMapping
    Response<PageResult<ActivityStageResponse>> searchActivityStages(@Valid @ModelAttribute ActivityStageSearchRequest request);

    /**
     * 查询活动展台详情。
     *
     * @param stageId 展台业务标识
     * @return 活动展台详情
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 展台不存在时抛出
     */
    @Operation(operationId = "getActivityStage", summary = "查询活动展台详情")
    @GetMapping("/{stageId}")
    Response<ActivityStageResponse> getActivityStage(@Positive(message = "展台标识必须为正数") @PathVariable("stageId") Long stageId);

    /**
     * 修改待上架活动展台。
     *
     * @param stageId 展台业务标识
     * @param request 修改请求
     * @return 修改后的展台配置
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 状态错误或版本冲突时抛出
     */
    @Operation(operationId = "updateActivityStage", summary = "修改待上架活动展台")
    @PutMapping(value = "/{stageId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<ActivityStageResponse> updateActivityStage(@Positive(message = "展台标识必须为正数") @PathVariable("stageId") Long stageId, @Valid @RequestBody UpdateActivityStageRequest request);

    /**
     * 预热并发布活动展台。
     *
     * @param stageId 展台业务标识
     * @param request 发布请求
     * @return 发布后的展台配置
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 活动不可发布、预热失败或版本冲突时抛出
     */
    @Operation(operationId = "publishActivityStage", summary = "发布活动展台")
    @PostMapping(value = "/{stageId}/publish", consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<ActivityStageResponse> publishActivityStage(@Positive(message = "展台标识必须为正数") @PathVariable("stageId") Long stageId, @Valid @RequestBody PublishActivityStageRequest request);
}
