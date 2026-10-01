package com.lavyoung.marketforge.api.strategy.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 策略奖品列表
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/09
 */
@Schema(
        name = "StrategyAwardResponse",
        description = "策略奖品配置"
)
public record StrategyAwardResponse(
        @Schema(
                description = "抽奖策略标识",
                example = "900901001"
        )
        Long strategyId,

        @Schema(
                description = "奖品标识",
                example = "900901011"
        )
        Long awardId,

        @Schema(
                description = "奖品标题",
                example = "随机矿石"
        )
        String awardTitle,

        @Schema(
                description = "奖品总库存",
                example = "1000",
                minimum = "0"
        )
        Integer awardCount,

        @Schema(
                description = "奖品剩余库存",
                example = "987",
                minimum = "0"
        )
        Integer awardCountSurplus,

        @Schema(
                description = "奖品中奖概率",
                example = "0.0100",
                minimum = "0"
        )
        BigDecimal awardRate,

        @Schema(
                description = "奖品展示排序，数值越小越靠前",
                example = "1"
        )
        Integer sort
) implements Serializable {
}
