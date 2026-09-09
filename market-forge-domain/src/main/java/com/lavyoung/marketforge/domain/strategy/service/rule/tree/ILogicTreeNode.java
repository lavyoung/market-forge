package com.lavyoung.marketforge.domain.strategy.service.rule.tree;

import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;

/**
 * 规则树节点执行接口。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
public interface ILogicTreeNode {

    /**
     * 执行当前规则节点。
     *
     * @param userId     用户标识
     * @param strategyId 抽奖策略标识
     * @param awardId    当前候选奖品标识
     * @param ruleValue  规则树节点配置值
     * @return 节点判断结果及当前奖品决策
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 规则配置无法解析时抛出
     */
    DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Long awardId, String ruleValue);

    /**
     * 获取当前节点对应的规则模型。
     *
     * @return 规则模型
     */
    RuleModel ruleModel();
}
