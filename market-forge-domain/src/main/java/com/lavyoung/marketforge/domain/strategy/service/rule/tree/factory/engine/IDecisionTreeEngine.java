package com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.engine;

import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.lavyoung.marketforge.types.exception.BusinessException;

/**
 * 规则树执行引擎-规则树组合接口
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
public interface IDecisionTreeEngine {

    /**
     * 从根节点开始执行规则树，直至没有可继续流转的节点。
     *
     * @param userId     参与抽奖的用户标识
     * @param strategyId 抽奖策略标识
     * @param awardId    责任链随机命中的初始奖品标识
     * @return 最后一个已执行节点产生的奖品决策结果
     * @throws BusinessException    节点结果无法匹配任何后继连线时抛出
     * @throws NullPointerException 规则树引用了未注册的节点实现时抛出
     */
    DefaultTreeFactory.StrategyAwardVO process(String userId, Long strategyId, Long awardId);
}
