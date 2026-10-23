package com.lavyoung.marketforge.api.activity.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 活动抽奖结果响应。
 *
 * <p>该响应只展示后端已经确认的抽奖结果，前端动画不得自行计算中奖奖品。
 * 活动、策略和奖品标识全部使用字符串返回，确保十八位业务标识能够被浏览器
 * 无损读取并在后续请求中原样回传。</p>
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
                description = "抽奖活动业务标识",
                example = "9007199254740993",
                type = "string"
        )
        String activityId,

        @Schema(
                description = "抽奖策略业务标识",
                example = "9007199254740994",
                type = "string"
        )
        String strategyId,

        @Schema(
                description = "命中奖品业务标识",
                example = "9007199254740995",
                type = "string"
        )
        String awardId,

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
