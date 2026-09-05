package com.lavyoung.marketforge.domain.strategy.model.entity;

import lombok.Builder;

/**
 * 执行抽奖所需的输入因子。
 *
 * @param userId     参与抽奖的用户标识
 * @param strategyId 本次抽奖使用的策略标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/04
 */
@Builder
public record RaffleFactorEntity(
        String userId,
        Long strategyId
) {

}
