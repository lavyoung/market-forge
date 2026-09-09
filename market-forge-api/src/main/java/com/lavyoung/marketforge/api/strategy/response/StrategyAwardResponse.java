package com.lavyoung.marketforge.api.strategy.response;

import java.math.BigDecimal;

/**
 * 策略奖品列表
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/09
 */
public record StrategyAwardResponse(
        Long strategyId,
        Long awardId,
        String awardTitle,
        Integer awardCount,
        Integer awardCountSurplus,
        BigDecimal awardRate,
        Integer sort
) {
}
