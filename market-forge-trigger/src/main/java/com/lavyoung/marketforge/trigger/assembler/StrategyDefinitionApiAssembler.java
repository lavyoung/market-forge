package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.strategy.request.*;
import com.lavyoung.marketforge.api.strategy.response.*;
import com.lavyoung.marketforge.application.strategy.model.*;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.model.PageRequest;
import com.lavyoung.marketforge.types.model.PageResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
    default StrategyRuleConfigCommand toRuleCommand(StrategyRuleConfigRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        return switch (request.ruleModel()) {
            case "rule_blacklist" -> new StrategyRuleConfigCommand(null, 1, request.ruleModel(),
                    encodeBlacklistRule(request.blacklist()), request.ruleDesc());
            case "rule_weight" -> new StrategyRuleConfigCommand(null, 1, request.ruleModel(),
                    encodeWeightRule(request.weight()), request.ruleDesc());
            case "rule_lock" -> new StrategyRuleConfigCommand(request.awardId(), 2, request.ruleModel(),
                    encodeLockRule(request.lock()), request.ruleDesc());
            default ->
                    throw new IllegalArgumentException("Unsupported configurable rule model: " + request.ruleModel());
        };
    }

    /**
     * 将黑名单结构化请求编码为当前 Application Command 使用的兼容格式。
     *
     * @param request 黑名单结构化配置
     * @return “兜底奖品标识:用户标识/用户标识”格式的规则值
     * @throws NullPointerException 黑名单配置为空时抛出
     */
    private String encodeBlacklistRule(BlacklistRuleConfigRequest request) {
        Objects.requireNonNull(request, "blacklist rule configuration must not be null");
        String userIds = request.userIds().stream().map(String::trim).collect(Collectors.joining(Constants.SPLIT));
        return request.fallbackAwardId() + Constants.COLON + userIds;
    }

    /**
     * 将权重档位结构化请求编码为当前 Application Command 使用的兼容格式。
     *
     * @param request 权重奖池结构化配置
     * @return “次数门槛:奖品标识/奖品标识;...”格式的规则值
     * @throws NullPointerException 权重配置为空时抛出
     */
    private String encodeWeightRule(WeightRuleConfigRequest request) {
        Objects.requireNonNull(request, "weight rule configuration must not be null");
        return request.tiers().stream().sorted((left, right) -> Long.compare(left.threshold(), right.threshold()))
                .map(tier -> tier.threshold() + Constants.COLON + tier.awardIds().stream()
                        .map(String::valueOf).collect(Collectors.joining(Constants.SPLIT)))
                .collect(Collectors.joining(Constants.SEMICOLON));
    }

    /**
     * 将次数锁结构化请求编码为当前 Application Command 使用的兼容格式。
     *
     * @param request 次数锁结构化配置
     * @return 非负整数格式的解锁次数
     * @throws NullPointerException 次数锁配置为空时抛出
     */
    private String encodeLockRule(LockRuleConfigRequest request) {
        Objects.requireNonNull(request, "lock rule configuration must not be null");
        return Integer.toString(request.requiredCount());
    }

    /**
     * 将完整应用结果转换为 API 响应。
     *
     * @param result 完整策略定义应用结果
     * @return 完整策略定义响应
     */
    StrategyDefinitionResponse toResponse(StrategyDefinitionResult result);

    /**
     * 将策略就绪检查应用结果转换为 REST 响应。
     *
     * <p>策略和奖品的 {@link Long} 标识只在该协议边界转换为字符串，
     * 防止浏览器使用 JavaScript number 接收十八位业务标识时丢失精度。</p>
     *
     * @param result 策略就绪检查应用结果
     * @return 对外策略就绪检查响应
     * @throws NullPointerException 应用结果为空时抛出
     */
    default StrategyReadinessResponse toReadinessResponse(StrategyReadinessResult result) {
        Objects.requireNonNull(result, "result must not be null");
        StrategyProbabilityPreviewResult preview = result.probabilityPreview();
        List<StrategyProbabilityIntervalResponse> intervals = preview.intervals().stream()
                .map(interval -> new StrategyProbabilityIntervalResponse(
                        interval.awardId().toString(), interval.weight(), interval.startInclusive(),
                        interval.endExclusive(), interval.probability()))
                .toList();
        StrategyProbabilityPreviewResponse previewResponse = new StrategyProbabilityPreviewResponse(
                preview.available(), preview.totalWeight(), intervals);
        return new StrategyReadinessResponse(
                result.strategyId().toString(), result.ready(), toIssueResponses(result.errors()),
                toIssueResponses(result.warnings()), toIssueResponses(result.suggestions()), previewResponse);
    }

    /**
     * 批量转换策略就绪检查问题。
     *
     * @param issues Application 问题结果
     * @return API 问题响应
     */
    default List<StrategyReadinessIssueResponse> toIssueResponses(List<StrategyReadinessIssueResult> issues) {
        return issues.stream()
                .map(issue -> new StrategyReadinessIssueResponse(issue.code(), issue.path(), issue.message()))
                .toList();
    }

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
