package com.lavyoung.marketforge.api.credit.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 积分兑换活动 SKU 请求。
 *
 * <p>用于承接外部客户端发起的积分兑换请求。接口层只表达协议入参，
 * 不承载业务编排逻辑；真正的积分扣减和 SKU 充值动作由 application 层完成。</p>
 *
 * @param userId        用户编号
 * @param sku           活动 SKU，决定兑换后给哪个活动增加抽奖次数
 * @param creditAmount  本次消耗的积分数量，必须为正数
 * @param outBusinessNo 外部业务幂等号，同一次兑换请求必须保持稳定
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

        @Schema(description = "消耗积分数量", example = "100")
        @NotNull(message = "消耗积分数量不能为空")
        @Positive(message = "消耗积分数量必须为正数")
        Long creditAmount,

        @Schema(description = "外部业务幂等号", example = "exchange-user-001-20261011-001")
        @NotBlank(message = "外部业务幂等号不能为空")
        String outBusinessNo
) {
}