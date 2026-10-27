package com.lavyoung.marketforge.api.activity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;

/**
 * 创建活动 SKU 请求。
 *
 * @param activityId      所属活动业务标识
 * @param activityCountId 兑换后获得的次数配置标识
 * @param stockCount      初始库存总量
 * @param creditAmount    兑换所需积分；为空表示不支持积分兑换
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
@Schema(description = "创建活动 SKU 请求")
public record CreateActivitySkuDefinitionRequest(
        @Schema(description = "所属活动业务标识", example = "100000000000000001", type = "string", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "所属活动不能为空")
        @Pattern(regexp = "^[1-9][0-9]{0,17}$", message = "活动标识格式不合法")
        String activityId,
        @Schema(description = "次数配置业务标识", example = "300000000000000001", type = "string", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "次数配置不能为空")
        @Pattern(regexp = "^[1-9][0-9]{0,17}$", message = "次数配置标识格式不合法")
        String activityCountId,
        @Schema(description = "初始库存总量", example = "1000", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @Positive(message = "初始库存必须为正数")
        long stockCount,
        @Schema(description = "兑换所需积分；为空表示关闭积分兑换", example = "100", minimum = "1", nullable = true)
        @Positive(message = "积分兑换价格必须为正数")
        Long creditAmount
) implements Serializable {
}
