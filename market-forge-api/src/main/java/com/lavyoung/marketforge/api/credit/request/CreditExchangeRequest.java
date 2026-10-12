package com.lavyoung.marketforge.api.credit.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 积分兑换活动 SKU 请求。
 *
 * <p>该请求只允许调用方指定“兑换什么商品”和“本次请求是谁发起的”。
 * SKU 的积分兑换价格由服务端根据运营配置查询，客户端不能传入价格，
 * 避免调用方篡改 {@code creditAmount} 后以低价兑换商品。</p>
 *
 * <p>接口层只表达 HTTP 协议和参数校验；待支付订单创建、服务端价格查询、
 * 积分扣减与异步发货由 application 层负责。</p>
 *
 * @param userId        用户编号
 * @param sku           活动 SKU，决定兑换后给哪个活动增加抽奖次数
 * @param outBusinessNo 外部业务幂等号；同一次兑换重试必须保持不变
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/12
 */
@Schema(description = "积分兑换活动 SKU 请求")
public record CreditExchangeRequest(

        @Schema(description = "用户编号", example = "user-001")
        @NotBlank(message = "用户编号不能为空")
        String userId,

        @Schema(description = "活动 SKU", example = "9011")
        @NotNull(message = "活动 SKU 不能为空")
        @Positive(message = "活动 SKU 必须为正数")
        Long sku,

        @Schema(
                description = "外部业务幂等号；相同业务重试时必须保持不变",
                example = "exchange-user-001-20261012-001"
        )
        @NotBlank(message = "外部业务幂等号不能为空")
        String outBusinessNo
) {
}
