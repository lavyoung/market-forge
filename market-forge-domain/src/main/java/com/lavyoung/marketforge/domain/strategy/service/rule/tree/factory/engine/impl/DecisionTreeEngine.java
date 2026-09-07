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
 * 规则树决策引擎。
 * <p>
 * 从规则树根节点开始逐节点执行，并依据节点结果匹配连线，直到到达叶子节点。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DecisionTreeEngine implements IDecisionTreeEngine {

    /**
     * 规则标识与规则树节点实现的映射。
     */
    private final Map<String, ILogicTreeNode> logicTreeNodeMap;

    /**
     * 当前引擎绑定的规则树配置。
     */
    private final RuleTreeVO ruleTreeVO;

    /**
     * {@inheritDoc}
     *
     * @throws BusinessException    节点执行结果无法匹配任何后继连线时抛出
     * @throws NullPointerException 规则树引用了未注册的节点实现时抛出
     */
    @Override
    public DefaultTreeFactory.StrategyAwardVO process(String userId, Long strategyId, Long awardId) {
        String rootRule = ruleTreeVO.treeRootRule();
        Map<String, RuleTreeNodeVO> treeNodeVOMap = ruleTreeVO.treeNodeVOMap();
        RuleTreeNodeVO nodeVO = treeNodeVOMap.get(rootRule);
        DefaultTreeFactory.StrategyAwardVO strategyAwardVO = null;
        while (nodeVO != null) {
            ILogicTreeNode treeNode = logicTreeNodeMap.get(nodeVO.ruleKey());
            DefaultTreeFactory.TreeActionEntity treeActionEntity = treeNode.logic(userId, strategyId, awardId);
            strategyAwardVO = treeActionEntity.strategyAwardVO();
            String nextNode = nextNode(treeActionEntity.ruleLogicCheckTypeVO().getCode(), nodeVO.ruleTreeNodeLineVoList());
            nodeVO = nextNode == null ? null : treeNodeVOMap.get(nextNode);
        }
        return strategyAwardVO;
    }

    /**
     * 根据当前节点结果选择下一跳节点。
     *
     * @param matterValue            当前节点输出的判断值
     * @param ruleTreeNodeLineVoList 当前节点配置的候选连线
     * @return 匹配连线的目标节点标识；没有后继连线时返回 {@code null}
     * @throws BusinessException 存在连线但没有任何条件匹配时抛出
     */
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

    /**
     * 判断节点结果是否满足指定连线条件。
     *
     * @param matterValue 当前节点输出的判断值
     * @param nodeLine    待匹配的节点连线配置
     * @return 满足连线限定条件时返回 {@code true}，否则返回 {@code false}
     * @throws NullPointerException 判断值或节点连线配置为 {@code null} 时抛出
     */
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
