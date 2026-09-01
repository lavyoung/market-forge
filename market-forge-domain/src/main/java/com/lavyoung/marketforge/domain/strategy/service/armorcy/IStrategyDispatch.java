package com.lavyoung.marketforge.domain.strategy.service.armorcy;

/**
 *
 * 策略调度接口
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
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
}
