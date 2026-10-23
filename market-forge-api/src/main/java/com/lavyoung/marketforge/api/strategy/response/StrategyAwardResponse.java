package com.lavyoung.marketforge.api.strategy.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 策略奖品配置响应。
 *
 * <p>策略标识和奖品标识属于业务标识符，不参与客户端数值计算，因此通过字符串返回，
 * 避免超过 JavaScript 安全整数范围后发生精度丢失。库存、概率和排序仍保持数值类型，
 * 便于客户端完成库存展示、概率格式化和列表排序。</p>
 *
 * @param strategyId        抽奖策略标识
 * @param awardId           奖品标识
 * @param awardTitle        奖品标题
 * @param awardCount        奖品库存总量
 * @param awardCountSurplus 奖品剩余库存
 * @param awardRate         奖品中奖概率
 * @param sort              奖品展示顺序
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/09
 */
@Schema(name = "StrategyAwardResponse", description = "策略奖品配置")
public record StrategyAwardResponse(
        @Schema(description = "抽奖策略标识", example = "9007199254740993", type = "string")
        String strategyId,

        @Schema(description = "奖品标识", example = "9007199254740994", type = "string")
        String awardId,

        @Schema(description = "奖品标题", example = "随机矿石")
        String awardTitle,

        @Schema(description = "奖品总库存", example = "1000", minimum = "0")
        Integer awardCount,

        @Schema(description = "奖品剩余库存", example = "987", minimum = "0")
        Integer awardCountSurplus,

        @Schema(description = "奖品中奖概率", example = "0.0100", minimum = "0")
        BigDecimal awardRate,

        @Schema(description = "奖品展示排序，数值越小越靠前", example = "1")
        Integer sort
) implements Serializable {
}
