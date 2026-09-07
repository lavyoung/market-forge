package com.lavyoung.marketforge.domain.strategy.model.vo;

import lombok.Builder;

/**
 * 规则树节点
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
@Builder
public record RuleTreeNodeLineVo(
        Integer treeId,
        String ruleNodeFrom,
        String ruleNodeTo,
        RuleLimitTypeVO ruleLimitTypeVO,
        RuleLogicCheckTypeVO ruleLimitValue
) {

}
