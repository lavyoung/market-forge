package com.lavyoung.marketforge.api.activity.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 活动抽奖响应。
 *
 * @param orderId    活动参与订单号
 * @param activityId 抽奖活动标识
 * @param strategyId 抽奖策略标识
 * @param awardId    命中奖品标识
 * @param awardKey   奖品业务标识
 * @param awardTitle 奖品标题
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/30
 */
@Schema(
        name = "ActivityDrawResponse",
        description = "活动抽奖结果"
)
public record ActivityDrawResponse(
        @Schema(
                description = "活动参与订单号",
                example = "AO100301202609301234"
        )
        String orderId,

        @Schema(
                description = "抽奖活动标识",
                example = "100301"
        )
        Long activityId,

        @Schema(
                description = "抽奖策略标识",
                example = "900901001"
        )
        Long strategyId,

        @Schema(
                description = "命中奖品标识",
                example = "900901011"
        )
        Long awardId,

        @Schema(
                description = "奖品业务标识",
                example = "random_ore"
        )
        String awardKey,

        @Schema(
                description = "奖品标题",
                example = "随机矿石"
        )
        String awardTitle
) implements Serializable {
}
