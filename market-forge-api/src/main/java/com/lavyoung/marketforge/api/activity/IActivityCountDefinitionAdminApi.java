package com.lavyoung.marketforge.api.activity;

import com.lavyoung.marketforge.api.activity.request.ActivityCountDefinitionSearchRequest;
import com.lavyoung.marketforge.api.activity.request.CreateActivityCountDefinitionRequest;
import com.lavyoung.marketforge.api.activity.request.UpdateActivityCountDefinitionRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityCountDefinitionResponse;
import com.lavyoung.marketforge.types.model.PageResult;
import com.lavyoung.marketforge.types.model.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 活动次数配置运营管理 REST API 契约。
 *
 * <p>提供次数配置分页、详情、创建和受保护修改能力。被开放活动引用的配置不能原地修改。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
@Tag(name = "运营后台活动次数配置", description = "管理活动 SKU 入账时增加的总、月、日抽奖额度")
@RequestMapping(path = IActivityCountDefinitionAdminApi.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface IActivityCountDefinitionAdminApi {

    /**
     * 活动次数配置管理基础路径。
     */
    String BASE_PATH = "/admin/activity-counts";

    /**
     * 创建活动次数配置。
     *
     * @param request 创建请求
     * @return 创建后的配置
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 配置非法、重复或写入失败时抛出
     */
    @Operation(operationId = "createActivityCountDefinition", summary = "创建活动次数配置")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<ActivityCountDefinitionResponse> createActivityCountDefinition(@Valid @RequestBody CreateActivityCountDefinitionRequest request);

    /**
     * 分页查询活动次数配置。
     *
     * @param request 查询和分页条件
     * @return 活动次数配置分页结果
     * @throws jakarta.validation.ValidationException 查询条件非法时抛出
     */
    @Operation(operationId = "searchActivityCountDefinitions", summary = "分页查询活动次数配置")
    @GetMapping
    Response<PageResult<ActivityCountDefinitionResponse>> searchActivityCountDefinitions(@Valid @ModelAttribute ActivityCountDefinitionSearchRequest request);

    /**
     * 查询活动次数配置详情。
     *
     * @param activityCountId 活动次数配置业务标识
     * @return 活动次数配置详情
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 配置不存在时抛出
     */
    @Operation(operationId = "getActivityCountDefinition", summary = "查询活动次数配置详情")
    @GetMapping("/{activityCountId}")
    Response<ActivityCountDefinitionResponse> getActivityCountDefinition(@Positive(message = "活动次数配置标识必须为正数") @PathVariable("activityCountId") Long activityCountId);

    /**
     * 修改活动次数配置。
     *
     * @param activityCountId 活动次数配置业务标识
     * @param request         修改请求
     * @return 修改后的配置
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 配置已被引用或版本冲突时抛出
     */
    @Operation(operationId = "updateActivityCountDefinition", summary = "修改活动次数配置")
    @PutMapping(value = "/{activityCountId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<ActivityCountDefinitionResponse> updateActivityCountDefinition(@Positive(message = "活动次数配置标识必须为正数") @PathVariable("activityCountId") Long activityCountId, @Valid @RequestBody UpdateActivityCountDefinitionRequest request);
}
