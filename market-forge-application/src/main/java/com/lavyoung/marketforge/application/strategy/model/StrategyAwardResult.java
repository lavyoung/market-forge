package com.lavyoung.marketforge.application.strategy.model;

import java.math.BigDecimal;

/**
 * 策略奖品查询结果。
 *
 * @param strategyId        抽奖策略标识
 * @param awardId           奖品标识
 * @param awardTitle        奖品标题
 * @param awardCount        奖品库存总量
 * @param awardCountSurplus 奖品库存剩余量
 * @param awardRate         奖品中奖概率
 * @param sort              奖品展示顺序
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/09
 */
public record StrategyAwardResult(
        Long strategyId,
        Long awardId,
        String awardTitle,
        Integer awardCount,
        Integer awardCountSurplus,
        BigDecimal awardRate,
        Integer sort
) {
}
