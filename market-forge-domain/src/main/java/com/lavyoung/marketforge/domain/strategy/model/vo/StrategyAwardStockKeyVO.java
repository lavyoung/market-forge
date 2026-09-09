package com.lavyoung.marketforge.domain.strategy.model.vo;

import lombok.Builder;

/**
 * 待同步的策略奖品库存标识。
 *
 * @param strategyId 策略标识
 * @param awardId    奖品标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/08
 */
@Builder
public record StrategyAwardStockKeyVO(
        Long strategyId,
        Long awardId
) {
}
