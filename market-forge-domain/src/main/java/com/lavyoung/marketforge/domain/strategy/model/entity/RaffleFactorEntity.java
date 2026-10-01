package com.lavyoung.marketforge.domain.strategy.model.entity;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 执行抽奖所需的输入因子。
 *
 * @param userId      参与抽奖的用户标识
 * @param strategyId  本次抽奖使用的策略标识
 * @param awardId     随机命中的奖品标识；抽奖前阶段可为空
 * @param endDateTime 活动结束时间，用于库存规则节点计算 Redis 锁租约；非活动抽奖时可为空
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/04
 */
@Builder
public record RaffleFactorEntity(
        String userId,
        Long strategyId,
        Long awardId,
        LocalDateTime endDateTime
) {

}
