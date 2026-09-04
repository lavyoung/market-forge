package com.lavyoung.marketforge.domain.strategy.service.armorcy;

/**
 * 抽奖策略调度接口。
 * <p>
 * 根据已装配的概率查找表执行普通抽奖或带权重门槛的抽奖。
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

    /**
     * 从指定权重门槛对应的已装配概率查找表中随机选取一个奖品。
     *
     * @param strategyId      策略标识
     * @param ruleWeightValue 当前命中的权重规则值
     * @return 随机选中的奖品标识
     * @throws IllegalArgumentException 策略尚未装配或随机数范围无效时抛出
     */
    long getRandomAwardIdAndWeight(Long strategyId, String ruleWeightValue);

}
