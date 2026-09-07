package com.lavyoung.marketforge.domain.strategy.model.entity;

import lombok.Builder;

/**
 * 规则树节点配置。
 * <p>
 * 记录节点所属规则树、节点执行的规则及其业务配置值。
 *
 * @param treeId    所属规则树标识
 * @param ruleKey   节点规则标识，用于定位对应的规则节点实现
 * @param ruleDesc  节点规则说明
 * @param ruleValue 节点规则配置值
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/07
 */
@Builder
public record RuleTreeNodeEntity(
        String treeId,
        String ruleKey,
        String ruleDesc,
        String ruleValue
) {
}
