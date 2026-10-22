package com.lavyoung.marketforge.api.activity;

import com.lavyoung.marketforge.api.activity.request.ActivityDefinitionSearchRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityDefinitionResponse;
import com.lavyoung.marketforge.types.model.PageResult;
import com.lavyoung.marketforge.types.model.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 活动定义运营查询 REST API 契约。
 *
 * <p>该接口查询 {@code raffle_activity} 中的活动定义，与负责渠道发布记录的
 * {@link IActivityStageAdminApi} 分离。当前只开放只读查询，后续创建、编辑和状态流转
 * 将继续扩展在同一资源路径下。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Tag(name = "运营后台活动管理", description = "提供活动定义查询能力")
@RequestMapping(path = IActivityDefinitionAdminApi.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface IActivityDefinitionAdminApi {

    /**
     * 运营后台活动定义基础路径。
     */
    String BASE_PATH = "/admin/activities";

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
}
