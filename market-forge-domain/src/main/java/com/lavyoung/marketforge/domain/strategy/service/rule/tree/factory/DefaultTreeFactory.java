package com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory;

import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.RuleTreeVO;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.ILogicTreeNode;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.engine.IDecisionTreeEngine;
import com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.engine.impl.DecisionTreeEngine;
import com.lavyoung.marketforge.types.domain.strategy.RuleModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 默认规则树决策引擎工厂。
 * <p>
 * 使用已注册的规则节点实现和指定规则树配置，创建一次规则树执行所需的决策引擎。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
@Slf4j
@Component
@AllArgsConstructor
public class DefaultTreeFactory {

    /**
     * 规则标识与规则树节点实现的映射。
     */
    private final Map<String, ILogicTreeNode> logicTreeNodeMap;

    /**
     * 根据规则树配置创建决策引擎。
     *
     * @param ruleTreeVO 待执行的完整规则树配置
     * @return 绑定节点注册表与规则树配置的决策引擎
     */
    public IDecisionTreeEngine openLogicTree(RuleTreeVO ruleTreeVO) {
        return new DecisionTreeEngine(logicTreeNodeMap, ruleTreeVO);
    }

    /**
     * 单个规则树节点的执行结果。
     *
     * @param ruleLogicCheckTypeVO 节点判断结果，用于匹配下一条连线
     * @param strategyAwardVO      节点计算得到的奖品结果
     */
    @Builder
    public record TreeActionEntity(
            RuleLogicCheckTypeVO ruleLogicCheckTypeVO,
            StrategyAwardVO strategyAwardVO
    ) {

    }


    /**
     * 规则树奖品决策结果。
     *
     * @param awardId       规则树最终确定的奖品标识
     * @param ruleModel     最终命中的规则模型
     * @param awardRuleValue 奖品规则附加配置
     */
    @Builder
    public record StrategyAwardVO(
            Long awardId,
            RuleModel ruleModel,
            String awardRuleValue
    ) {

    }
}
