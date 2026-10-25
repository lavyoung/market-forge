package com.lavyoung.marketforge.domain.strategy.service.armorcy;

import com.lavyoung.marketforge.types.exception.BusinessException;

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
     * 从指定权重门槛对应的已装配概率快照中随机选取一个奖品。
     *
     * <p>权重概率快照使用“策略标识_次数门槛”作为装配键，因此调用方必须
     * 传递命中的档位门槛，不能传递该档位中的奖品列表。</p>
     *
     * @param strategyId      策略标识
     * @param weightThreshold 当前命中的权重次数门槛
     * @return 随机选中的奖品标识
     * @throws IllegalArgumentException 权重门槛为空时抛出
     * @throws BusinessException        对应权重概率快照尚未装配时抛出
     */
    long getRandomAwardIdAndWeight(Long strategyId, String weightThreshold);

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
