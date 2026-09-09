package com.lavyoung.marketforge.api.strategy.response;

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
public record StrategyRaffleResponse(
        Long strategyId,
        Long awardId,
        String awardKey,
        String awardConfig,
        String awardDesc
) implements Serializable {
}
