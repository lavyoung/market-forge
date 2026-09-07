package com.lavyoung.marketforge.infrastructure.persistent.po;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 规则树节点连线持久化对象。
 * <p>
 * 映射节点之间的流转方向以及触发该流转的比较条件。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/07
 */
@Getter
@Setter
@NoArgsConstructor
public class RuleTreeNodeLinePO extends BasePO {

    /**
     * 数据库主键。
     */
    private Long id;

    /**
     * 所属规则树标识。
     */
    private String treeId;

    /**
     * 来源节点规则标识。
     */
    private String ruleNodeFrom;

    /**
     * 目标节点规则标识。
     */
    private String ruleNodeTo;

    /**
     * 条件比较类型。
     */
    private String ruleLimitType;

    /**
     * 条件期望值。
     */
    private String ruleLimitValue;
}
