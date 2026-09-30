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
public record ActivityDrawResponse(
        @Schema(description = "", example = "")
        String orderId,
        @Schema(description = "", example = "")
        Long activityId,
        @Schema(description = "", example = "")
        Long strategyId,
        @Schema(description = "", example = "")
        Long awardId,
        @Schema(description = "", example = "")
        String awardKey,
        @Schema(description = "", example = "")
        String awardTitle
) implements Serializable {
}
