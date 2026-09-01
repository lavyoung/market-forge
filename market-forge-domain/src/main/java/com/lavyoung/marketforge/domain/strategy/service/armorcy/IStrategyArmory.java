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
     * 触发时机为活动审核通过时调用
     *
     * @param strategyId 策略标识
     * @throws ArithmeticException 奖品概率无法形成有效概率范围时抛出
     */
    boolean assembleLotteryStrategy(Long strategyId);

}
