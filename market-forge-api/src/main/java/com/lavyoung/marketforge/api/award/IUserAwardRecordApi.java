package com.lavyoung.marketforge.api.award;

import com.lavyoung.marketforge.api.award.request.UserAwardRecordSearchRequest;
import com.lavyoung.marketforge.api.award.response.UserAwardRecordResponse;
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
 * 用户中奖记录运营查询 REST API 契约。
 *
 * <p>该接口将分散在不同物理库中的用户中奖记录，通过 Elasticsearch
 * 统一索引提供分页检索能力，避免运营查询轮询所有分库。</p>
 *
 * <p>接口只提供只读查询，不修改中奖记录状态，也不参与发奖流程。
 * Elasticsearch 中的数据由 Canal 基于 MySQL Binlog 异步同步，
 * 因此查询结果允许存在短暂的最终一致性延迟。</p>
 *
 * <p>中奖记录包含用户、活动和奖品信息，属于运营数据，所有接口必须经过
 * 运营后台身份认证，不允许通过公开业务接口匿名访问。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/16
 */
@Tag(name = "用户中奖记录", description = "提供面向运营场景的用户中奖记录聚合查询能力")
@RequestMapping(path = IUserAwardRecordApi.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface IUserAwardRecordApi {

    /**
     * 用户中奖记录运营查询接口基础路径。
     *
     * <p>{@code /admin} 前缀用于将接口纳入运营后台 Spring Security
     * 过滤链，防止匿名客户端读取用户中奖数据。</p>
     */
    String BASE_PATH = "/admin/award/records";

    /**
     * 分页检索用户中奖记录。
     *
     * <p>查询结果默认按照中奖时间倒序排列。中奖时间相同时，
     * 基础设施查询实现继续使用来源库和分片主键稳定排序，
     * 避免翻页过程中记录顺序漂移。</p>
     *
     * @param request 用户中奖记录查询条件
     * @return 包含当前页记录与分页元数据的统一响应
     * @throws IllegalArgumentException 查询时间范围或分页参数非法时抛出
     * @throws com.lavyoung.marketforge.types.exception.BusinessException
     *         Elasticsearch 查询失败或当前用户未通过认证时抛出
     */
    @Operation(
            operationId = "searchUserAwardRecords",
            summary = "分页查询用户中奖记录",
            description = "按照用户、奖品、发奖状态和中奖时间查询跨分库聚合后的中奖记录。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "查询条件或分页参数非法"),
            @ApiResponse(responseCode = "401", description = "运营人员尚未登录"),
            @ApiResponse(responseCode = "403", description = "运营人员没有查询权限"),
            @ApiResponse(responseCode = "500", description = "搜索服务查询失败")
    })
    @GetMapping
    Response<PageResult<UserAwardRecordResponse>> searchUserAwardRecords(@Valid @ModelAttribute UserAwardRecordSearchRequest request);
}
