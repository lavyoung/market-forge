package com.lavyoung.marketforge.application.strategy.model;

/**
 * 抽奖应用用例的执行结果。
 *
 * @param strategyId  抽奖策略标识
 * @param awardId     奖品标识
 * @param awardKey    奖品业务标识
 * @param awardConfig 奖品发放配置
 * @param awardDesc   奖品说明
 */
public record RaffleResult(
        Long strategyId,
        Long awardId,
        String awardKey,
        String awardConfig,
        String awardDesc
) {
}
