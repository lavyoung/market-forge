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

import java.util.*;

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
     * @throws BusinessException 规则树缺少节点实现、节点结果无效、流转不匹配或存在循环时抛出
     */
    @Override
    public DefaultTreeFactory.StrategyAwardVO process(String userId, Long strategyId, Long awardId) {
        String rootRule = ruleTreeVO.treeRootRule();
        Map<String, RuleTreeNodeVO> treeNodeVOMap = ruleTreeVO.treeNodeVOMap();
        RuleTreeNodeVO nodeVO = treeNodeVOMap.get(rootRule);
        if (nodeVO == null) {
            throw invalidTree("规则树根节点不存在 rootRule=" + rootRule);
        }
        DefaultTreeFactory.StrategyAwardVO strategyAwardVO = null;
        Set<String> visitedNodes = new HashSet<>();
        while (nodeVO != null) {
            if (!visitedNodes.add(nodeVO.ruleKey())) {
                throw invalidTree("规则树存在循环节点 ruleKey=" + nodeVO.ruleKey());
            }
            ILogicTreeNode treeNode = logicTreeNodeMap.get(nodeVO.ruleKey());
            if (treeNode == null) {
                throw invalidTree("规则树节点未注册 ruleKey=" + nodeVO.ruleKey());
            }
            DefaultTreeFactory.TreeActionEntity treeActionEntity = treeNode.logic(userId, strategyId, awardId, nodeVO.ruleValue());
            if (treeActionEntity == null || treeActionEntity.ruleLogicCheckTypeVO() == null
                    || treeActionEntity.strategyAwardVO() == null) {
                throw invalidTree("规则树节点返回结果不完整 ruleKey=" + nodeVO.ruleKey());
            }
            strategyAwardVO = treeActionEntity.strategyAwardVO();
            Optional<String> nextNode = nextNode(
                    treeActionEntity.ruleLogicCheckTypeVO().getCode(),
                    nodeVO.ruleTreeNodeLineVoList()
            );
            if (nextNode.isEmpty()) {
                nodeVO = null;
            } else {
                String nextRuleKey = nextNode.get();
                nodeVO = treeNodeVOMap.get(nextRuleKey);
                if (nodeVO == null) {
                    throw invalidTree("规则树后继节点不存在 ruleKey=" + nextRuleKey);
                }
            }
        }
        return strategyAwardVO;
    }

    /**
     * 根据当前节点结果选择下一跳节点。
     *
     * @param matterValue            当前节点输出的判断值
     * @param ruleTreeNodeLineVoList 当前节点配置的候选连线
     * @return 匹配连线的目标节点标识；没有后继连线时返回空
     * @throws BusinessException 存在连线但没有任何条件匹配时抛出
     */
    private Optional<String> nextNode(String matterValue, List<RuleTreeNodeLineVo> ruleTreeNodeLineVoList) {
        if (CollectionUtils.isEmpty(ruleTreeNodeLineVoList)) {
            return Optional.empty();
        }
        for (RuleTreeNodeLineVo ruleTreeNodeLineVo : ruleTreeNodeLineVoList) {
            if (decisionLogic(matterValue, ruleTreeNodeLineVo)) {
                String nextRuleKey = ruleTreeNodeLineVo.ruleNodeTo();
                if (nextRuleKey == null || nextRuleKey.isBlank()) {
                    throw invalidTree("规则树连线目标节点为空");
                }
                return Optional.of(nextRuleKey);
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
     * @throws BusinessException 判断值、限定类型或限定值为空时抛出
     */
    public boolean decisionLogic(String matterValue, RuleTreeNodeLineVo nodeLine) {
        if (matterValue == null || nodeLine == null || nodeLine.ruleLimitTypeVO() == null
                || nodeLine.ruleLimitValue() == null) {
            throw invalidTree("规则树连线条件不完整");
        }
        switch (nodeLine.ruleLimitTypeVO()) {
            case EQ -> {
                return matterValue.equals(nodeLine.ruleLimitValue().getCode());
            }
            default -> throw invalidTree("暂不支持的规则树连线限定类型 type=" + nodeLine.ruleLimitTypeVO());
        }
    }

    /**
     * 记录规则树配置错误并创建统一业务异常。
     *
     * @param message 可定位配置问题的日志信息
     * @return 规则值配置异常
     */
    private BusinessException invalidTree(String message) {
        log.error(message);
        return new BusinessException(BusinessResponseCode.STRATEGY_RULE_VALUE_INVALID);
    }
}
