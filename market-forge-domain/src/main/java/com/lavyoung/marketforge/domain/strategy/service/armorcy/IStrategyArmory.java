package com.lavyoung.marketforge.domain.strategy.service.armorcy;


/**
 * 策略接口
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 */
public interface IStrategyArmory {

    /**
     * 根据奖品概率配置装配指定抽奖策略的概率查找表。
     *
     * @param strategyId 策略标识
     * @throws ArithmeticException 奖品概率无法形成有效概率范围时抛出
     */
    void assembleLotteryStrategy(Long strategyId);

    /**
     * 从已装配的概率查找表中随机选取一个奖品。
     *
     * @param strategyId 策略标识
     * @return 随机选中的奖品标识
     * @throws IllegalArgumentException 策略尚未装配或随机数范围无效时抛出
     */
    int getRandomAwardId(Long strategyId);
}
