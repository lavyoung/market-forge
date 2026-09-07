package com.lavyoung.marketforge.domain.strategy.model.vo;

import lombok.Builder;

import java.util.List;

/**
 * 规则树节点
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
@Builder
public record RuleTreeNodeVO(
        Integer treeId,
        String ruleKey,
        String ruleDesc,
        String ruleValue,
        List<RuleTreeNodeLineVo> ruleTreeNodeLineVoList
) {


}
