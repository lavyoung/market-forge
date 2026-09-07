package com.lavyoung.marketforge.domain.strategy.model.entity;

import lombok.Builder;

/**
 * 规则树基础配置。
 * <p>
 * 描述一棵规则树的业务标识、用途以及执行时进入的根节点。
 *
 * @param treeId          规则树唯一标识
 * @param treeDesc        规则树说明
 * @param treeNodeRuleKey 根节点规则标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/07
 */
@Builder
public record RuleTreeEntity(
        String treeId,
        String treeDesc,
        String treeNodeRuleKey
) {
}
