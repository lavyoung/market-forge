package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.activity.request.ActivityOrderSearchRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityOrderResponse;
import com.lavyoung.marketforge.application.activity.model.ActivityOrderSearchItem;
import com.lavyoung.marketforge.application.activity.model.ActivityOrderSearchQuery;
import com.lavyoung.marketforge.types.model.PageResult;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.Objects;

/**
 * 活动订单运营查询协议装配器。
 *
 * <p>负责 API HTTP 模型与 Application 查询模型之间的转换。
 * 该装配器不执行查询、不校验领域规则，也不接触 Elasticsearch。</p>
 *
 * <p>应用层使用 {@link Long} 表达活动、SKU 和策略标识，而 API 响应使用
 * {@link String}，避免 JavaScript 接收超大整数时发生精度丢失。
 * MapStruct 在协议边界完成该类型转换。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/18
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ActivityOrderSearchAssembler {

    /**
     * 将活动订单 HTTP 查询请求转换为应用查询条件。
     *
     * @param request 活动订单 HTTP 查询请求
     * @return 活动订单应用层查询条件
     */
    ActivityOrderSearchQuery toQuery(ActivityOrderSearchRequest request);

    /**
     * 将单条活动订单应用查询结果转换为 API 响应。
     *
     * <p>其中 {@code activityId}、{@code sku} 和 {@code strategyId}
     * 会由数值转换为字符串。</p>
     *
     * @param item 活动订单应用查询结果
     * @return 活动订单 API 响应
     */
    ActivityOrderResponse toResponse(ActivityOrderSearchItem item);

    /**
     * 批量转换活动订单查询结果。
     *
     * @param items 活动订单应用查询结果列表
     * @return 活动订单 API 响应列表
     */
    List<ActivityOrderResponse> toResponses(List<ActivityOrderSearchItem> items);

    /**
     * 将应用层分页结果转换为 API 分页结果。
     *
     * <p>该方法保留总记录数、页码和每页记录数，仅转换当前页中的记录类型。</p>
     *
     * @param pageResult 活动订单应用分页结果
     * @return 活动订单 API 分页结果
     * @throws NullPointerException 分页结果为空时抛出
     */
    default PageResult<ActivityOrderResponse> toPageResponse(PageResult<ActivityOrderSearchItem> pageResult) {
        Objects.requireNonNull(pageResult, "pageResult must not be null");
        return new PageResult<>(
                toResponses(pageResult.records()),
                pageResult.total(),
                pageResult.pageNumber(),
                pageResult.pageSize()
        );
    }
}
