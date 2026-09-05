package com.lavyoung.marketforge.domain.strategy.model.entity;

import lombok.Builder;

/**
 * 抽奖结果中的奖品信息。
 *
 * @param strategyId  产生本次抽奖结果的策略标识
 * @param awardId     奖品标识
 * @param awardKey    奖品业务键
 * @param awardConfig 奖品发放配置
 * @param awardDesc   奖品描述
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/04
 */
@Builder
public record RaffleAwardEntity(
        Long strategyId,
        Long awardId,
        String awardKey,
        String awardConfig,
        String awardDesc
) {

}
