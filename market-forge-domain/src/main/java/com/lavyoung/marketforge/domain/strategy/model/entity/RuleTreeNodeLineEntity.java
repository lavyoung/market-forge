package com.lavyoung.marketforge.domain.strategy.model.entity;

import lombok.Builder;

/**
 * 规则树节点连线配置。
 * <p>
 * 根据来源节点的执行结果及限定条件，声明规则树的下一跳节点。
 *
 * @param treeId         所属规则树标识
 * @param ruleNodeFrom   来源节点规则标识
 * @param ruleNodeTo     目标节点规则标识
 * @param ruleLimitType  条件比较类型
 * @param ruleLimitValue 条件期望值
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/07
 */
@Builder
public record RuleTreeNodeLineEntity(
        String treeId,
        String ruleNodeFrom,
        String ruleNodeTo,
        String ruleLimitType,
        String ruleLimitValue
) {
}
