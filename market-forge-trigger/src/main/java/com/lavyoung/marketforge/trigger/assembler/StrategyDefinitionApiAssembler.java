package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.strategy.request.*;
import com.lavyoung.marketforge.api.strategy.response.StrategyAwardConfigResponse;
import com.lavyoung.marketforge.api.strategy.response.StrategyDefinitionResponse;
import com.lavyoung.marketforge.api.strategy.response.StrategyDefinitionSummaryResponse;
import com.lavyoung.marketforge.api.strategy.response.StrategyRuleConfigResponse;
import com.lavyoung.marketforge.application.strategy.model.*;
import com.lavyoung.marketforge.types.model.PageRequest;
import com.lavyoung.marketforge.types.model.PageResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Objects;

/**
 * 策略定义 API 协议装配器。
 *
 * <p>负责 REST 请求、Application Command、Application Result 和 REST 响应之间
 * 的边界转换。路径策略标识和认证操作人由 Controller 显式传入，不能来自请求体。</p>
 *
 * <p>应用层的 {@link Long} 业务标识在响应边界转换为字符串，避免浏览器使用
 * JavaScript number 解析十八位编号时发生精度丢失。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR, imports = PageRequest.class)
public interface StrategyDefinitionApiAssembler {

    /**
     * 将 HTTP 查询参数转换为应用查询条件。
     *
     * @param request 策略分页查询请求
     * @return 策略应用查询条件
     */
    @Mapping(target = "page", expression = "java(new PageRequest(request.pageNumber(), request.pageSize()))")
    StrategyDefinitionSearchQuery toSearchQuery(StrategyDefinitionSearchRequest request);

    /**
     * 将创建请求转换为应用命令。
     *
     * @param request  创建策略请求
     * @param operator 当前认证运营人员
     * @return 策略创建命令
     */
    @Mapping(target = "operator", source = "operator")
    CreateStrategyDefinitionCommand toCreateCommand(CreateStrategyDefinitionRequest request, String operator);

    /**
     * 将修改请求与路径标识、认证操作人组合为应用命令。
     *
     * @param strategyId 路径策略标识
     * @param request    修改策略请求
     * @param operator   当前认证运营人员
     * @return 策略修改命令
     */
    @Mapping(target = "strategyId", source = "strategyId")
    @Mapping(target = "operator", source = "operator")
    UpdateStrategyDefinitionCommand toUpdateCommand(Long strategyId, UpdateStrategyDefinitionRequest request, String operator);

    /**
     * 创建复制历史策略的应用命令。
     *
     * @param sourceStrategyId 被复制的策略标识
     * @param operator         当前认证运营人员
     * @return 复制策略命令
     */
    @Mapping(target = "sourceStrategyId", source = "sourceStrategyId")
    @Mapping(target = "operator", source = "operator")
    CopyStrategyDefinitionCommand toCopyCommand(Long sourceStrategyId, String operator);

    /**
     * 将状态请求与路径标识、认证操作人组合为应用命令。
     *
     * @param strategyId 路径策略标识
     * @param request    状态变更请求
     * @param operator   当前认证运营人员
     * @return 策略状态变更命令
     */
    @Mapping(target = "strategyId", source = "strategyId")
    @Mapping(target = "operator", source = "operator")
    ChangeStrategyDefinitionStateCommand toStateCommand(Long strategyId, ChangeStrategyDefinitionStateRequest request, String operator);

    /**
     * 将奖品请求项转换为应用命令项。
     *
     * @param request 奖品请求项
     * @return 奖品应用命令项
     */
    StrategyAwardConfigCommand toAwardCommand(StrategyAwardConfigRequest request);

    /**
     * 将规则请求项转换为应用命令项。
     *
     * @param request 规则请求项
     * @return 规则应用命令项
     */
    StrategyRuleConfigCommand toRuleCommand(StrategyRuleConfigRequest request);

    /**
     * 将完整应用结果转换为 API 响应。
     *
     * @param result 完整策略定义应用结果
     * @return 完整策略定义响应
     */
    StrategyDefinitionResponse toResponse(StrategyDefinitionResult result);

    /**
     * 将策略应用列表项转换为 API 摘要响应。
     *
     * @param item 策略应用列表项
     * @return 策略 API 摘要响应
     */
    StrategyDefinitionSummaryResponse toSummaryResponse(StrategyDefinitionSearchItem item);

    /**
     * 批量转换策略应用列表项。
     *
     * @param items 策略应用列表项
     * @return 策略 API 摘要响应列表
     */
    List<StrategyDefinitionSummaryResponse> toSummaryResponses(List<StrategyDefinitionSearchItem> items);

    /**
     * 转换策略分页记录类型并保留分页元数据。
     *
     * @param pageResult 策略应用分页结果
     * @return 策略 API 分页结果
     * @throws NullPointerException 分页结果为空时抛出
     */
    default PageResult<StrategyDefinitionSummaryResponse> toPageResponse(PageResult<StrategyDefinitionSearchItem> pageResult) {
        Objects.requireNonNull(pageResult, "pageResult must not be null");
        return new PageResult<>(toSummaryResponses(pageResult.records()), pageResult.total(), pageResult.pageNumber(), pageResult.pageSize());
    }

    /**
     * 将策略奖品应用结果转换为 API 响应。
     *
     * @param result 策略奖品应用结果
     * @return 策略奖品响应
     */
    StrategyAwardConfigResponse toAwardResponse(StrategyAwardConfigResult result);

    /**
     * 将策略规则应用结果转换为 API 响应。
     *
     * @param result 策略规则应用结果
     * @return 策略规则响应
     */
    StrategyRuleConfigResponse toRuleResponse(StrategyRuleConfigResult result);
}
