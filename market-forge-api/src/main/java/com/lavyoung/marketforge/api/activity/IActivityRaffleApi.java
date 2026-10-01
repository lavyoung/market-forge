package com.lavyoung.marketforge.api.activity;

import com.lavyoung.marketforge.api.activity.request.ActivityAwardListRequest;
import com.lavyoung.marketforge.api.activity.request.ActivityDrawRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityAwardResponse;
import com.lavyoung.marketforge.api.activity.response.ActivityDrawResponse;
import com.lavyoung.marketforge.types.model.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 活动抽奖 REST API 契约。
 * <p>
 * 该接口以活动为入口对外提供抽奖用例，包括活动运行时缓存装配和用户抽奖。
 * Controller 只负责协议适配，具体业务流程由 application 层编排完成。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Tag(name = "活动抽奖", description = "提供活动维度的装配和抽奖能力")
@RequestMapping(path = IActivityRaffleApi.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface IActivityRaffleApi {


    String BASE_PATH = "/activity/raffles";

    /**
     * 装配活动抽奖所需的运行时缓存。
     * <p>
     * 该接口以活动标识为入口，预热活动 SKU、活动次数配置、活动详情以及绑定的策略缓存。
     * 通常在活动上线、配置变更或压测前调用，避免首次用户请求时承担装配成本。
     *
     * @param activityId 抽奖活动标识
     * @return 装配结果
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 活动不存在、活动未配置 SKU 或策略装配失败时抛出
     */
    @PutMapping("/armory")
    Response<Void> armory(@RequestParam("activityId") Long activityId);

    /**
     * 执行一次活动抽奖。
     * <p>
     * 流程包括参数校验、活动参与订单创建、策略抽奖、中奖记录落库、可靠消息任务创建、
     * 参与订单状态更新，最终返回本次命中奖品信息。
     *
     * @param request 活动抽奖请求
     * @return 本次抽奖结果
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 活动不可参与、额度不足、策略配置异常或中奖记录保存失败时抛出
     */
    @PostMapping(path = "/draw", consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<ActivityDrawResponse> draw(@Valid @RequestBody ActivityDrawRequest request);

    /**
     * 查询活动奖品列表。
     * <p>
     * 这个接口用于前端活动抽奖页展示奖品列表。
     * 它会以用户和活动为查询入口，返回每个奖品的解锁状态。
     *
     * @param request 活动奖品列表查询请求
     * @return 活动奖品展示列表
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 活动不存在、活动未绑定策略或规则配置异常时抛出
     */
    @Operation(
            operationId = "queryActivityAwardList",
            summary = "查询活动奖品列表",
            description = "按用户和活动查询奖品展示信息，并返回次数锁解锁状态。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "请求体缺失、格式错误或字段校验失败"),
            @ApiResponse(responseCode = "422", description = "活动不存在、活动未绑定策略或规则配置异常"),
            @ApiResponse(responseCode = "500", description = "系统内部错误")
    })
    @PostMapping(path = "/awards", consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<List<ActivityAwardResponse>> queryAwardList(@Valid @RequestBody ActivityAwardListRequest request);
}
