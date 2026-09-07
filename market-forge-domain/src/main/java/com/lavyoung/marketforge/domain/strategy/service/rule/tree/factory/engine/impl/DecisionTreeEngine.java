package com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.engine.impl;

import com.lavyoung.marketforge.domain.strategy.model.vo.RuleTreeNodeLineVo;
import com.lavyoung.marketforge.domain.strategy.model.vo.RuleTreeNodeVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.RuleTreeVO;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.engine.IDecisionTreeEngine;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * 决策树引擎
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DecisionTreeEngine implements IDecisionTreeEngine {

    private final Map<String, ILogicTreeNode> logicTreeNodeMap;
    private final RuleTreeVO ruleTreeVO;

    @Override
    public DefaultTreeFactory.StrategyAwardData process(String userId, Long strategyId, Long awardId) {
        String rootRule = ruleTreeVO.treeRootRule();
        Map<String, RuleTreeNodeVO> treeNodeVOMap = ruleTreeVO.treeNodeVOMap();
        RuleTreeNodeVO nodeVO = treeNodeVOMap.get(rootRule);
        DefaultTreeFactory.StrategyAwardData strategyAwardData = null;
        while (nodeVO != null) {
            ILogicTreeNode treeNode = logicTreeNodeMap.get(nodeVO.ruleKey());
            DefaultTreeFactory.TreeActionEntity treeActionEntity = treeNode.logic(userId, strategyId, awardId);
            strategyAwardData = treeActionEntity.strategyAwardData();
            String nextNode = nextNode(treeActionEntity.ruleLogicCheckTypeVO().getCode(), nodeVO.ruleTreeNodeLineVoList());
            nodeVO = treeNodeVOMap.get(nextNode);
        }
        return strategyAwardData;
    }

    private String nextNode(String matterValue, List<RuleTreeNodeLineVo> ruleTreeNodeLineVoList) {
        if (CollectionUtils.isEmpty(ruleTreeNodeLineVoList)) {
            return null;
        }
        for (RuleTreeNodeLineVo ruleTreeNodeLineVo : ruleTreeNodeLineVoList) {
            if (decisionLogic(matterValue, ruleTreeNodeLineVo)) {
                return ruleTreeNodeLineVo.ruleNodeTo();
            }
        }
        log.error("规则树-节点决策失败 未正确配置节点流转规则");
        throw new BusinessException(BusinessResponseCode.STRATEGY_RULE_VALUE_INVALID);
    }

    public boolean decisionLogic(String matterValue, RuleTreeNodeLineVo nodeLine) {
        switch (nodeLine.ruleLimitTypeVO()) {
            case EQ -> {
                return matterValue.equals(nodeLine.ruleLimitValue().getCode());
            }
            case GE -> {
                // todo 暂时无需实现
                return false;
            }
            case GT -> {
                //
                return false;
            }
            default -> {
                return false;
            }
        }
    }
}
