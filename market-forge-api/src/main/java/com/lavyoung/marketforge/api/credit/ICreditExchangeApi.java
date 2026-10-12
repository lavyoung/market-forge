package com.lavyoung.marketforge.api.credit;

import com.lavyoung.marketforge.api.credit.request.CreditExchangeRequest;
import com.lavyoung.marketforge.types.model.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 积分兑换 REST API 契约。
 *
 * <p>该接口用于承接外部用户的积分兑换请求。接口层只定义 HTTP 协议、
 * 参数校验和统一响应格式，不承载待支付下单、积分扣减、异步发货和事务控制等业务流程。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
@Tag(name = "积分兑换", description = "提供用户使用积分兑换活动 SKU 的能力")
@RequestMapping(path = ICreditExchangeApi.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface ICreditExchangeApi {


    /**
     * 积分兑换接口基础路径。
     */
    String BASE_PATH = "/credit/exchanges";

    /**
     * 使用积分兑换活动 SKU。
     *
     * <p>调用方传入用户、活动 SKU、消耗积分和外部业务幂等号。
     * 服务端会先创建积分支付类型的待支付活动 SKU 订单，再扣减用户积分；
     * 积分扣减成功后通过可靠消息异步完成发货入账。</p>
     *
     * @param request 积分兑换请求
     * @return 活动 SKU 订单号
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 参数非法、积分不足、SKU 不可用或充值失败时抛出
     */
    @Operation(
            operationId = "exchangeCreditForSku",
            summary = "积分兑换活动 SKU",
            description = "用户消耗积分兑换活动 SKU 对应的抽奖次数。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "兑换成功"),
            @ApiResponse(responseCode = "400", description = "请求体缺失、格式错误或字段校验失败"),
            @ApiResponse(responseCode = "422", description = "积分不足、SKU 不可用或兑换失败"),
            @ApiResponse(responseCode = "500", description = "系统内部错误")
    })
    @PostMapping(path = "/sku", consumes = MediaType.APPLICATION_JSON_VALUE)
    Response<String> exchangeCreditForSku(@Valid @RequestBody CreditExchangeRequest request);
}
