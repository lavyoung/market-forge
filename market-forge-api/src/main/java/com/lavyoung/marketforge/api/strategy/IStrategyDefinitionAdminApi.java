package com.lavyoung.marketforge.api.strategy;

import com.lavyoung.marketforge.api.strategy.request.ChangeStrategyDefinitionStateRequest;
import com.lavyoung.marketforge.api.strategy.request.CreateStrategyDefinitionRequest;
import com.lavyoung.marketforge.api.strategy.request.StrategyDefinitionSearchRequest;
import com.lavyoung.marketforge.api.strategy.request.UpdateStrategyDefinitionRequest;
import com.lavyoung.marketforge.api.strategy.response.StrategyDefinitionResponse;
import com.lavyoung.marketforge.api.strategy.response.StrategyDefinitionSummaryResponse;
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
 * 策略定义运营管理 REST API 契约。
 *
 * <p>负责策略草稿创建、完整详情、草稿修改、历史策略复制和生命周期管理。
 * 已生效策略不能原地修改，需要通过复制接口生成新的独立草稿。</p>
 *
 * <p>接口与运行时快照刷新接口共享 {@code /admin/strategies} 资源根路径，
 * 但分别表达配置生命周期和运行时维护职责。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Tag(name = "运营后台策略管理", description = "提供策略定义配置、复制、发布、停用和运行时快照维护能力")
@RequestMapping(path = IStrategyDefinitionAdminApi.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface IStrategyDefinitionAdminApi {

    /**
     * 策略运营管理基础路径。
     */
    String BASE_PATH = "/admin/strategies";

    /**
     * 创建策略草稿。
     *
     * @param request 创建策略请求
     * @return 创建后的完整策略定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 配置非法、标识重复或保存失败时抛出
     */
    @Operation(operationId = "createStrategyDefinition", summary = "创建策略定义")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "400", description = "请求或策略配置非法"),
            @ApiResponse(responseCode = "401", description = "运营人员尚未登录"),
            @ApiResponse(responseCode = "403", description = "没有操作权限或 CSRF 校验失败"),
            @ApiResponse(responseCode = "409", description = "策略标识冲突")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<StrategyDefinitionResponse> createStrategyDefinition(@Valid @RequestBody CreateStrategyDefinitionRequest request);

    /**
     * 分页查询策略定义摘要。
     *
     * <p>该接口只返回策略主表摘要，不在列表查询中加载奖品和规则子表。
     * 查询结果按照更新时间和数据库主键倒序排列。</p>
     *
     * @param request 策略定义分页查询条件
     * @return 满足条件的策略定义摘要分页结果
     * @throws jakarta.validation.ValidationException                     查询条件或分页参数不合法时抛出
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 数据库查询失败时抛出
     */
    @Operation(operationId = "searchStrategyDefinitions", summary = "分页查询策略定义")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "查询条件或分页参数非法"),
            @ApiResponse(responseCode = "401", description = "运营人员尚未登录"),
            @ApiResponse(responseCode = "403", description = "没有查询权限")
    })
    @GetMapping
    Response<PageResult<StrategyDefinitionSummaryResponse>> searchStrategyDefinitions(@Valid @ModelAttribute StrategyDefinitionSearchRequest request);

    /**
     * 查询策略完整定义。
     *
     * @param strategyId 策略业务标识
     * @return 包含策略根、奖品和规则的完整定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 策略不存在时抛出
     */
    @Operation(operationId = "getStrategyDefinition", summary = "查询策略定义详情")
    @GetMapping("/{strategyId}")
    Response<StrategyDefinitionResponse> getStrategyDefinition(@Positive(message = "策略标识必须为正数") @PathVariable("strategyId") Long strategyId);

    /**
     * 修改完整草稿策略。
     *
     * @param strategyId 策略业务标识
     * @param request    修改请求
     * @return 修改后的完整策略定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 策略不可编辑、配置非法或版本冲突时抛出
     */
    @Operation(operationId = "updateStrategyDefinition", summary = "修改草稿策略定义")
    @PutMapping(value = "/{strategyId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<StrategyDefinitionResponse> updateStrategyDefinition(@Positive(message = "策略标识必须为正数") @PathVariable("strategyId") Long strategyId, @Valid @RequestBody UpdateStrategyDefinitionRequest request);

    /**
     * 将历史策略复制为新的独立草稿。
     *
     * @param strategyId 被复制的策略业务标识
     * @return 新生成的策略草稿
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 原策略不存在或仍可原地编辑时抛出
     */
    @Operation(operationId = "copyStrategyDefinition", summary = "复制历史策略为新草稿")
    @PostMapping("/{strategyId}/copies")
    Response<StrategyDefinitionResponse> copyStrategyDefinition(@Positive(message = "策略标识必须为正数") @PathVariable("strategyId") Long strategyId);

    /**
     * 发布或停用策略定义。
     *
     * @param strategyId 策略业务标识
     * @param request    状态变更请求
     * @return 状态变更后的完整策略定义
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 发布校验、预热、状态迁移或版本更新失败时抛出
     */
    @Operation(operationId = "changeStrategyDefinitionState", summary = "变更策略定义状态")
    @PatchMapping(value = "/{strategyId}/state", consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<StrategyDefinitionResponse> changeStrategyDefinitionState(@Positive(message = "策略标识必须为正数") @PathVariable("strategyId") Long strategyId, @Valid @RequestBody ChangeStrategyDefinitionStateRequest request);
}
