package com.lavyoung.marketforge.domain.strategy.service.rule.tree.impl;

import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import org.springframework.stereotype.Component;

/**
 * 规则树默认终止节点。
 * <p>
 * 当前置规则全部放行时保留原候选奖品，并以叶子节点结果结束规则树执行。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/09
 */
@Component("default")
public class DefaultLogicTreeNode implements ILogicTreeNode {

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Long awardId, String ruleValue) {
        return DefaultTreeFactory.TreeActionEntity.builder()
                .ruleLogicCheckTypeVO(RuleLogicCheckTypeVO.ALLOW)
                .strategyAwardVO(DefaultTreeFactory.StrategyAwardVO.builder()
                        .awardId(awardId)
                        .ruleModel(ruleModel())
                        .build())
                .build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RuleModel ruleModel() {
        return RuleModel.DEFAULT;
    }
}
