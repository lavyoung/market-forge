package com.lavyoung.marketforge.infrastructure.persistent.po;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 规则树节点持久化对象。
 * <p>
 * 映射节点所属规则树、节点规则及对应的业务配置值。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/07
 */
@Getter
@Setter
@NoArgsConstructor
public class RuleTreeNodePO extends BasePO {

    /**
     * 数据库主键。
     */
    private Long id;

    /**
     * 所属规则树标识。
     */
    private String treeId;

    /**
     * 节点规则标识。
     */
    private String ruleKey;

    /**
     * 节点规则说明。
     */
    private String ruleDesc;

    /**
     * 节点规则配置值。
     */
    private String ruleValue;
}
