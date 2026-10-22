package com.lavyoung.marketforge.api.award;

import com.lavyoung.marketforge.api.award.request.AwardDefinitionSearchRequest;
import com.lavyoung.marketforge.api.award.request.ChangeAwardDefinitionStateRequest;
import com.lavyoung.marketforge.api.award.request.CreateAwardDefinitionRequest;
import com.lavyoung.marketforge.api.award.request.UpdateAwardDefinitionRequest;
import com.lavyoung.marketforge.api.award.response.AwardDefinitionResponse;
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
 * 奖品定义运营管理 REST API 契约。
 *
 * <p>该接口负责奖品定义的创建、详情查询、草稿编辑和生命周期变更。
 * 所有写操作都需要运营人员登录，并通过 CSRF 校验。</p>
 *
 * <p>奖品启用后不能直接修改核心发奖配置。需要修改时，应创建新的奖品定义，
 * 避免历史中奖记录的履约含义发生变化。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Tag(name = "运营后台奖品管理", description = "提供奖品定义创建、查询、编辑、启用和停用能力")
@RequestMapping(path = IAwardDefinitionAdminApi.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface IAwardDefinitionAdminApi {

    /**
     * 奖品定义运营管理基础路径。
     */
    String BASE_PATH = "/admin/awards";

    /**
     * 创建奖品定义草稿。
     *
     * @param request 创建奖品定义请求
     * @return 创建完成后的完整奖品定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 奖品编码重复、配置非法或持久化失败时抛出
     */
    @Operation(operationId = "createAwardDefinition", summary = "创建奖品定义")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "请求参数或奖品配置非法"),
            @ApiResponse(responseCode = "401", description = "运营人员尚未登录"),
            @ApiResponse(responseCode = "403", description = "没有操作权限或 CSRF 校验失败"),
            @ApiResponse(responseCode = "409", description = "奖品业务编码已经存在")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<AwardDefinitionResponse> createAwardDefinition(@Valid @RequestBody CreateAwardDefinitionRequest request);

    /**
     * 分页查询奖品定义。
     *
     * <p>过滤条件通过 URL 查询参数传递。奖品编码使用精确匹配，
     * 奖品名称使用前缀匹配，查询结果按照更新时间和数据库主键倒序排列。</p>
     *
     * <p>该接口只提供运营查询能力，不触发奖品创建、状态变更或发奖流程。</p>
     *
     * @param request 奖品定义分页查询条件
     * @return 满足条件的奖品定义分页结果
     * @throws jakarta.validation.ValidationException                     查询条件或分页参数不合法时抛出
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 数据库查询失败时抛出
     */
    @Operation(operationId = "searchAwardDefinitions", summary = "分页查询奖品定义")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "查询条件或分页参数非法"),
            @ApiResponse(responseCode = "401", description = "运营人员尚未登录"),
            @ApiResponse(responseCode = "403", description = "没有查询权限")
    })
    @GetMapping
    Response<PageResult<AwardDefinitionResponse>> searchAwardDefinitions(@Valid @ModelAttribute AwardDefinitionSearchRequest request);

    /**
     * 查询指定奖品定义详情。
     *
     * @param awardId 奖品业务标识
     * @return 奖品定义详情
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 奖品不存在时抛出
     */
    @Operation(operationId = "getAwardDefinition", summary = "查询奖品定义详情")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "奖品标识非法"),
            @ApiResponse(responseCode = "401", description = "运营人员尚未登录"),
            @ApiResponse(responseCode = "403", description = "没有查询权限"),
            @ApiResponse(responseCode = "404", description = "奖品定义不存在")
    })
    @GetMapping("/{awardId}")
    Response<AwardDefinitionResponse> getAwardDefinition(@Positive(message = "奖品标识必须为正数") @PathVariable("awardId") Long awardId);

    /**
     * 修改草稿奖品定义。
     *
     * @param awardId 奖品业务标识
     * @param request 草稿修改请求
     * @return 修改后的完整奖品定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 奖品不是草稿、版本冲突或配置非法时抛出
     */
    @Operation(operationId = "updateAwardDefinition", summary = "修改草稿奖品定义")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "修改成功"),
            @ApiResponse(responseCode = "400", description = "请求参数或奖品状态非法"),
            @ApiResponse(responseCode = "401", description = "运营人员尚未登录"),
            @ApiResponse(responseCode = "403", description = "没有操作权限或 CSRF 校验失败"),
            @ApiResponse(responseCode = "404", description = "奖品定义不存在"),
            @ApiResponse(responseCode = "409", description = "奖品定义已经被其他操作修改")
    })
    @PutMapping(value = "/{awardId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<AwardDefinitionResponse> updateAwardDefinition(@Positive(message = "奖品标识必须为正数") @PathVariable("awardId") Long awardId,
                                                            @Valid @RequestBody UpdateAwardDefinitionRequest request);

    /**
     * 变更奖品定义生命周期状态。
     *
     * <p>当前支持 {@code draft -> active -> disabled}，不允许回退状态。</p>
     *
     * @param awardId 奖品业务标识
     * @param request 状态变更请求
     * @return 状态变更后的完整奖品定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 状态迁移非法或版本冲突时抛出
     */
    @Operation(operationId = "changeAwardDefinitionState", summary = "变更奖品定义状态")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "状态变更成功"),
            @ApiResponse(responseCode = "400", description = "状态迁移非法"),
            @ApiResponse(responseCode = "401", description = "运营人员尚未登录"),
            @ApiResponse(responseCode = "403", description = "没有操作权限或 CSRF 校验失败"),
            @ApiResponse(responseCode = "404", description = "奖品定义不存在"),
            @ApiResponse(responseCode = "409", description = "奖品定义已经被其他操作修改")
    })
    @PatchMapping(value = "/{awardId}/state", consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<AwardDefinitionResponse> changeAwardDefinitionState(@Positive(message = "奖品标识必须为正数") @PathVariable("awardId") Long awardId,
                                                                 @Valid @RequestBody ChangeAwardDefinitionStateRequest request);
}
