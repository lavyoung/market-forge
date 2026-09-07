package com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory;

import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.RuleTreeVO;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.engine.IDecisionTreeEngine;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.engine.impl.DecisionTreeEngine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 规则树工厂
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
@Slf4j
@Component
@AllArgsConstructor
public class DefaultTreeFactory {

    private final Map<String, ILogicTreeNode> logicTreeNodeMap;

    public IDecisionTreeEngine openLogicTree(RuleTreeVO ruleTreeVO) {
        return new DecisionTreeEngine(logicTreeNodeMap, ruleTreeVO);
    }

    /**
     * 执行结果
     *
     * @param ruleLogicCheckTypeVO
     * @param strategyAwardData
     */
    @Builder
    public record TreeActionEntity(
            RuleLogicCheckTypeVO ruleLogicCheckTypeVO,
            StrategyAwardData strategyAwardData
    ) {

    }


    /**
     * 策略奖品
     *
     * @param awardId
     * @param awardRuleValue
     */
    @Builder
    public record StrategyAwardData(
            Integer awardId,
            String awardRuleValue
    ) {

    }
}
