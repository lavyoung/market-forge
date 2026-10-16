package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.award.request.UserAwardRecordSearchRequest;
import com.lavyoung.marketforge.api.award.response.UserAwardRecordResponse;
import com.lavyoung.marketforge.application.award.model.UserAwardRecordSearchItem;
import com.lavyoung.marketforge.application.award.model.UserAwardRecordSearchQuery;
import com.lavyoung.marketforge.types.model.PageResult;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Objects;

/**
 * <p>负责将 API 层接收的 HTTP 查询参数转换为 Application 层查询对象。
 * 该装配器只完成协议模型与应用模型之间的字段映射，不校验发奖业务规则，
 * 也不构造 Elasticsearch 查询条件。</p>
 *
 * <p>当前两个模型的字段名称和类型保持一致，MapStruct 可以自动完成映射。
 * 如果后续 API 字段为了前端语义发生变化，应当在本装配器中显式声明映射，
 * 避免 HTTP 协议变化扩散到 Application 层。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/16
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserAwardRecordSearchAssembler {


    /**
     * 将用户中奖记录 HTTP 查询请求转换为应用层查询条件。
     *
     * @param request 用户中奖记录 HTTP 查询请求
     * @return 用户中奖记录应用层查询条件
     */
    UserAwardRecordSearchQuery toQuery(UserAwardRecordSearchRequest request);

    /**
     * 将单条应用查询结果转换为 API 响应。
     *
     * @param item 用户中奖记录应用查询结果
     * @return 用户中奖记录 API 响应
     */
    UserAwardRecordResponse toResponse(UserAwardRecordSearchItem item);

    /**
     * 批量转换用户中奖记录。
     *
     * @param items 用户中奖记录应用查询结果列表
     * @return 用户中奖记录 API 响应列表
     */
    List<UserAwardRecordResponse> toResponses(List<UserAwardRecordSearchItem> items);

    /**
     * 将应用层分页结果转换为 API 分页结果。
     *
     * <p>分页元数据保持不变，只转换当前页记录类型：</p>
     *
     * <pre>
     * PageResult&lt;UserAwardRecordSearchItem&gt;
     *     -> PageResult&lt;UserAwardRecordResponse&gt;
     * </pre>
     *
     * @param pageResult 用户中奖记录应用分页结果
     * @return 用户中奖记录 API 分页结果
     * @throws NullPointerException 分页结果为空时抛出
     */
    default PageResult<UserAwardRecordResponse> toPageResponse(PageResult<UserAwardRecordSearchItem> pageResult) {
        Objects.requireNonNull(pageResult, "pageResult must not be null");
        return new PageResult<>(
                toResponses(pageResult.records()),
                pageResult.total(),
                pageResult.pageNumber(),
                pageResult.pageSize()
        );
    }
}
