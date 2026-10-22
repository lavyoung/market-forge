package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.award.request.AwardDefinitionSearchRequest;
import com.lavyoung.marketforge.api.award.request.ChangeAwardDefinitionStateRequest;
import com.lavyoung.marketforge.api.award.request.CreateAwardDefinitionRequest;
import com.lavyoung.marketforge.api.award.request.UpdateAwardDefinitionRequest;
import com.lavyoung.marketforge.api.award.response.AwardDefinitionResponse;
import com.lavyoung.marketforge.application.award.model.*;
import com.lavyoung.marketforge.types.model.PageRequest;
import com.lavyoung.marketforge.types.model.PageResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Objects;

/**
 * 奖品定义 API 协议装配器。
 *
 * <p>负责 API 请求、Application Command、Application Result 和 API 响应
 * 之间的边界转换。该装配器不校验领域规则，也不访问数据库。</p>
 *
 * <p>奖品标识和操作人不由请求体决定。奖品标识来自 HTTP 路径，
 * 操作人来自已经通过认证的后台会话。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR, imports = PageRequest.class)
public interface AwardDefinitionApiAssembler {

    /**
     * 将奖品定义 HTTP 查询请求转换为应用层查询条件。
     *
     * <p>API 中独立的页码和每页记录数在协议边界组合为共享分页值对象，
     * 后续 Application 和 Infrastructure 层不再感知 HTTP 参数结构。</p>
     *
     * @param request 奖品定义 HTTP 查询请求
     * @return 奖品定义应用查询条件
     */
    @Mapping(target = "page", expression = "java(new PageRequest(request.pageNumber(), request.pageSize()))")
    AwardDefinitionSearchQuery toSearchQuery(AwardDefinitionSearchRequest request);

    /**
     * 将创建请求转换为应用层创建命令。
     *
     * @param request  创建奖品定义请求
     * @param operator 当前登录运营人员标识
     * @return 奖品定义创建命令
     */
    @Mapping(target = "operator", source = "operator")
    CreateAwardDefinitionCommand toCreateCommand(CreateAwardDefinitionRequest request, String operator);

    /**
     * 将草稿修改请求转换为应用层修改命令。
     *
     * @param awardId  HTTP 路径中的奖品标识
     * @param request  草稿修改请求
     * @param operator 当前登录运营人员标识
     * @return 奖品定义修改命令
     */
    @Mapping(target = "awardId", source = "awardId")
    @Mapping(target = "operator", source = "operator")
    UpdateAwardDefinitionCommand toUpdateCommand(Long awardId, UpdateAwardDefinitionRequest request, String operator);

    /**
     * 将状态变更请求转换为应用层状态变更命令。
     *
     * @param awardId  HTTP 路径中的奖品标识
     * @param request  状态变更请求
     * @param operator 当前登录运营人员标识
     * @return 奖品定义状态变更命令
     */
    @Mapping(target = "awardId", source = "awardId")
    @Mapping(target = "operator", source = "operator")
    ChangeAwardDefinitionStateCommand toStateCommand(Long awardId, ChangeAwardDefinitionStateRequest request, String operator);

    /**
     * 将应用层结果转换为 API 响应。
     *
     * <p>MapStruct 会在协议边界把 {@code Long awardId} 转换为字符串，
     * 防止 JavaScript 解析十八位奖品编号时发生精度丢失。</p>
     *
     * @param result 奖品定义应用结果
     * @return 奖品定义 API 响应
     */
    AwardDefinitionResponse toResponse(AwardDefinitionResult result);

    /**
     * 批量转换奖品定义应用结果。
     *
     * @param results 奖品定义应用结果列表
     * @return 奖品定义 API 响应列表
     */
    List<AwardDefinitionResponse> toResponses(List<AwardDefinitionResult> results);

    /**
     * 将应用分页结果转换为 API 分页结果。
     *
     * <p>只转换当前页记录类型，总记录数、页码和每页记录数保持不变。</p>
     *
     * @param pageResult 奖品定义应用分页结果
     * @return 奖品定义 API 分页结果
     * @throws NullPointerException 分页结果为空时抛出
     */
    default PageResult<AwardDefinitionResponse> toPageResponse(PageResult<AwardDefinitionResult> pageResult) {
        Objects.requireNonNull(pageResult, "pageResult must not be null");
        return new PageResult<>(toResponses(pageResult.records()), pageResult.total(), pageResult.pageNumber(), pageResult.pageSize());
    }
}
