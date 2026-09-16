package com.lavyoung.marketforge.api.strategy.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 策略抽奖结果契约。
 *
 * @param strategyId  抽奖策略标识
 * @param awardId     命中奖品标识
 * @param awardKey    奖品业务标识
 * @param awardConfig 奖品发放配置
 * @param awardDesc   奖品说明
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/09
 */
@Schema(
        name = "StrategyRaffleResponse",
        description = "策略抽奖结果"
)
public record StrategyRaffleResponse(

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
                description = "奖品发放配置",
                example = "quantity=1"
        )
        String awardConfig,

        @Schema(
                description = "奖品说明",
                example = "随机矿石"
        )
        String awardDesc

) implements Serializable {
}
