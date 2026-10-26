package com.lavyoung.marketforge.application.strategy.model;

import java.math.BigDecimal;

/**
 * 策略奖品的整数概率命中区间。
 *
 * <p>区间采用左闭右开形式，例如 {@code [20, 50)} 表示随机整数
 * 落在 20 至 49 时命中对应奖品。</p>
 *
 * @param awardId        奖品业务标识
 * @param weight         约简后的整数权重
 * @param startInclusive 命中区间起点
 * @param endExclusive   命中区间终点
 * @param probability    奖品理论命中概率
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
public record StrategyProbabilityIntervalResult(
        Long awardId,
        int weight,
        int startInclusive,
        int endExclusive,
        BigDecimal probability
) {
}
