package com.lavyoung.marketforge.domain.strategy.service.armorcy;

import java.time.LocalDateTime;

/**
 * 抽奖策略调度接口。
 * <p>
 * 只负责抽奖的处理 不关心使用方或者调用抽奖的初始化。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/08/31
 */
public interface IStrategyDispatch {

    /**
     * 从已装配的概率查找表中随机选取一个奖品。
     *
     * @param strategyId 策略标识
     * @return 随机选中的奖品标识
     * @throws IllegalArgumentException 策略尚未装配或随机数范围无效时抛出
     */
    long getRandomAwardId(Long strategyId);

    /**
     * 从指定权重门槛对应的已装配概率查找表中随机选取一个奖品。
     *
     * @param strategyId      策略标识
     * @param ruleWeightValue 当前命中的权重规则值
     * @return 随机选中的奖品标识
     * @throws IllegalArgumentException 策略尚未装配或随机数范围无效时抛出
     */
    long getRandomAwardIdAndWeight(Long strategyId, String ruleWeightValue);

    /**
     * 原子扣减指定策略奖品的一份 Redis 库存。
     *
     * @param strategyId  策略标识
     * @param awardId     奖品标识
     * @param endDateTime 活动结束时间，用于计算库存锁租约；为空时使用默认短租约
     * @return 库存充足并扣减成功返回 {@code true}
     */
    boolean subtractAwardStock(Long strategyId, Long awardId, LocalDateTime endDateTime);

    /**
     * 原子扣减指定策略奖品的一份 Redis 库存。
     * <p>
     * 兼容不携带活动结束时间的调用，下游会使用默认短租约保护 Redis 锁。
     *
     * @param strategyId 策略标识
     * @param awardId    奖品标识
     * @return 库存充足并扣减成功返回 {@code true}
     */
    default boolean subtractAwardStock(Long strategyId, Long awardId) {
        return subtractAwardStock(strategyId, awardId, null);
    }
}
