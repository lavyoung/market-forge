package com.lavyoung.marketforge.domain.strategy.service.rule.chain;

import com.lavyoung.marketforge.domain.strategy.service.rule.chain.factory.DefaultChainFactory;

/**
 * 抽奖规则责任链节点。
 * <p>
 * 每个节点负责一种抽奖前规则，节点可直接返回奖品，也可将请求传递给后继节点。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
public interface ILogicChain extends ILogicChainArmory {

    /**
     * 执行当前节点的抽奖规则。
     *
     * @param userId     参与抽奖的用户标识
     * @param strategyId 抽奖策略标识
     * @return 当前节点或后继节点选中的奖品及命中规则模型
     */
    DefaultChainFactory.StrategyAwardVO logic(String userId, Long strategyId);
}
