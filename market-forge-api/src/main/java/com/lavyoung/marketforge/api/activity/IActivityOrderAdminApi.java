package com.lavyoung.marketforge.api.activity;

import com.lavyoung.marketforge.api.activity.request.ActivityOrderSearchRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityOrderResponse;
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
 * 活动订单运营查询 REST API 契约。
 *
 * <p>该接口面向运营后台，通过 Elasticsearch 聚合查询分散在不同物理库中的
 * 活动订单。接口只负责只读检索，不参与订单创建、额度入账或状态流转。</p>
 *
 * <p>MySQL 与 Elasticsearch 通过 Canal 异步同步，因此查询结果允许存在短暂的
 * 最终一致性延迟。需要强一致性的交易判断不得依赖该接口。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/18
 */
@Tag(name = "运营后台活动订单", description = "提供活动订单跨分库聚合查询能力")
@RequestMapping(path = IActivityOrderAdminApi.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface IActivityOrderAdminApi {

    /**
     * 活动订单运营查询基础路径。
     */
    String BASE_PATH = "/admin/activity/orders";

    /**
     * 分页检索活动订单。
     *
     * <p>查询结果按照下单时间倒序排列。下单时间相同时，基础设施实现需要继续使用
     * 来源库、分片主键等稳定字段排序，防止翻页时记录顺序漂移。</p>
     *
     * @param request 活动订单查询条件
     * @return 包含当前页订单及分页元数据的统一响应
     * @throws IllegalArgumentException                                   查询条件或分页参数非法时抛出
     * @throws com.lavyoung.marketforge.types.exception.BusinessException Elasticsearch 查询失败时抛出
     */
    @Operation(operationId = "searchActivityOrders", summary = "分页查询活动订单", description = "按照用户、活动、订单号、状态和下单时间查询跨分库聚合后的活动订单。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "查询条件或分页参数非法"),
            @ApiResponse(responseCode = "401", description = "运营人员尚未登录"),
            @ApiResponse(responseCode = "403", description = "运营人员没有查询权限"),
            @ApiResponse(responseCode = "500", description = "搜索服务查询失败")
    })
    @GetMapping
    Response<PageResult<ActivityOrderResponse>> searchActivityOrders(@Valid @ModelAttribute ActivityOrderSearchRequest request);
}
