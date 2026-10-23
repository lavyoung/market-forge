package com.lavyoung.marketforge.api.strategy.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 策略抽奖结果响应。
 *
 * <p>活动、策略和奖品标识在服务端内部仍使用 {@link Long}，只在 HTTP 协议出口转换为
 * 字符串。这样既保留服务端数据库查询和业务关联能力，也避免浏览器解析大整数时发生
 * 精度丢失。</p>
 *
 * @param orderId     抽奖参与订单号
 * @param activityId  抽奖活动标识
 * @param strategyId  抽奖策略标识
 * @param awardId     命中奖品标识
 * @param awardKey    奖品业务标识
 * @param awardConfig 奖品发放配置
 * @param awardDesc   奖品说明
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/09
 */
@Schema(name = "StrategyRaffleResponse", description = "策略抽奖结果")
public record StrategyRaffleResponse(
        @Schema(description = "抽奖参与订单号", example = "RO100301202609301234")
        String orderId,

        @Schema(description = "抽奖活动标识", example = "9007199254740993", type = "string")
        String activityId,

        @Schema(description = "抽奖策略标识", example = "9007199254740994", type = "string")
        String strategyId,

        @Schema(description = "命中奖品标识", example = "9007199254740995", type = "string")
        String awardId,

        @Schema(description = "奖品业务标识", example = "random_ore")
        String awardKey,

        @Schema(description = "奖品发放配置", example = "quantity=1")
        String awardConfig,

        @Schema(description = "奖品说明", example = "随机矿石")
        String awardDesc
) implements Serializable {
}
