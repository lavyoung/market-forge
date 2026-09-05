package com.lavyoung.marketforge.domain.strategy.service.armorcy;


/**
 * 策略装备接口
 * <p>
 * 负责抽奖前策略的初始化工作 不负责抽奖的处理工作
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
     * @return 策略及其权重规则全部装配成功时返回 {@code true}
     * @throws ArithmeticException 奖品概率无法形成有效概率范围时抛出
     */
    boolean assembleLotteryStrategy(Long strategyId);

}
