package com.lavyoung.marketforge.domain.strategy.service.rule.tree;

import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;

/**
 * 规则树接口
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
public interface ILogicTreeNode {

    DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Long awardId);
}
