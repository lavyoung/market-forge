package com.lavyoung.marketforge.domain.strategy.service.raffle;

import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleAwardEntity;
import com.lavyoung.marketforge.domain.strategy.model.entity.RaffleFactorEntity;

/**
 * 抽奖策略领域服务。
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 * @date 2026/09/04
 */
public interface IRaffleStrategy {

    /**
     * 根据抽奖因子执行规则判断和抽奖计算。
     *
     * @param raffleFactorEntity 包含用户和策略标识的抽奖因子
     * @return 本次抽奖命中的奖品信息
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 抽奖因子不符合业务要求时抛出
     */
    RaffleAwardEntity performRaffle(RaffleFactorEntity raffleFactorEntity);
}
