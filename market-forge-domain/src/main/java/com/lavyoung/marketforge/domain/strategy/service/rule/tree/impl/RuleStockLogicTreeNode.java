package com.lavyoung.marketforge.domain.strategy.service.rule.tree.impl;

import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 奖品库存规则树节点。
 * <p>
 * 校验命中奖品的库存规则，并返回是否接管后续规则树流程的判断结果。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleStockLogicTreeNode implements ILogicTreeNode {

    /**
     * {@inheritDoc}
     */
    @Override
    public DefaultTreeFactory.TreeActionEntity logic(String userId, Long strategyId, Long awardId) {
        return DefaultTreeFactory.TreeActionEntity
                .builder()
                .strategyAwardVO(DefaultTreeFactory.StrategyAwardVO.builder()
                        .awardId(null)
                        .awardRuleValue("1/100")
                        .build())
                .ruleLogicCheckTypeVO(RuleLogicCheckTypeVO.TAKE_OVER)
                .build();
    }
}
