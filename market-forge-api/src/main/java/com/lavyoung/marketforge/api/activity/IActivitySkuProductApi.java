package com.lavyoung.marketforge.api.activity;

import com.lavyoung.marketforge.api.activity.response.ActivitySkuProductResponse;
import com.lavyoung.marketforge.types.model.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 活动 SKU 商品查询 REST API 契约。
 *
 * <p>该接口面向活动商品列表页，按活动标识查询可兑换的 SKU 商品。
 * 返回内容包括 SKU 库存、兑换后获得的抽奖次数和服务端配置的积分价格。</p>
 *
 * <p>接口只承担查询职责，不创建订单、不扣减积分，也不修改 SKU 库存。
 * 真正的兑换操作由积分兑换接口处理。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/12
 */
@Tag(name = "活动 SKU 商品", description = "提供活动下可兑换 SKU 商品的查询能力")
@RequestMapping(path = IActivitySkuProductApi.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface IActivitySkuProductApi {

    /**
     * 活动 SKU 商品查询接口基础路径。
     */
    String BASE_PATH = "/activity/sku-products";

    /**
     * 查询指定活动配置的全部 SKU 商品。
     *
     * <p>前端进入活动兑换页时调用该接口，获取可展示的商品列表。
     * 每个商品包含库存、兑换后入账的抽奖次数，以及服务端配置的积分兑换价格。</p>
     *
     * @param activityId 抽奖活动标识
     * @return 指定活动下的 SKU 商品列表；活动未配置 SKU 时返回空列表
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 当活动标识非法或查询活动配置失败时抛出
     */
    @Operation(
            operationId = "queryActivitySkuProducts",
            summary = "查询活动 SKU 商品列表",
            description = "按活动标识查询可使用积分兑换的 SKU 商品、库存、抽奖次数和积分价格。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "活动标识为空或不是正整数"),
            @ApiResponse(responseCode = "500", description = "系统内部错误")
    })
    @GetMapping
    Response<List<ActivitySkuProductResponse>> querySkuProducts(@RequestParam("activityId")
                                                                @NotNull(message = "活动标识不能为空")
                                                                @Positive(message = "活动标识必须为正数")
                                                                Long activityId);
}
