package com.lavyoung.marketforge.api.activity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;

/**
 * 活动抽奖请求。
 * <p>
 * 用户从某个活动 SKU 参与抽奖。SKU 用于确认本次消耗的活动权益来源，
 * 活动标识用于校验 SKU 是否属于当前活动，避免跨活动错误参与。
 *
 * @param userId     参与抽奖的用户标识
 * @param activityId 抽奖活动标识
 * @param sku        活动 SKU
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/30
 */
@Schema(
        name = "ActivityDrawRequest",
        description = "活动抽奖请求"
)
public record ActivityDrawRequest(
        @Schema(
                description = "参与抽奖的用户标识",
                example = "user-001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "用户标识不能为空")
        String userId,

        @Schema(
                description = "抽奖活动标识",
                example = "100301",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "1"
        )
        @NotNull(message = "活动标识不能为空")
        @Positive(message = "活动标识必须大于零")
        Long activityId,

        @Schema(
                description = "活动 SKU",
                example = "9011",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "1"
        )
        @NotNull(message = "活动 SKU 不能为空")
        @Positive(message = "活动 SKU 必须大于零")
        Long sku
) implements Serializable {

}
