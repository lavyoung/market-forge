package com.lavyoung.marketforge.domain.strategy.model.vo;

import lombok.Builder;

import java.util.Map;

/**
 * 规则数对象【注意：不具有唯一ID，不需要改变数据结果的对象，可以被定义为值对象】
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/06
 */
@Builder
public record RuleTreeVO(
        Integer treeId, // 规则数id
        String treeName,  //
        String treeDesc,
        String treeRootRule, // 根节点
        Map<String, RuleTreeNodeVO> treeNodeVOMap
) {
}
