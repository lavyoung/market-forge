package com.lavyoung.marketforge.infrastructure.persistent.po;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 规则树主配置持久化对象。
 * <p>
 * 映射规则树的业务标识、说明以及执行入口节点。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/07
 */
@Getter
@Setter
@NoArgsConstructor
public class RuleTreePO extends BasePO {

    /**
     * 数据库主键。
     */
    private Long id;

    /**
     * 规则树唯一标识。
     */
    private String treeId;

    /**
     * 规则树说明。
     */
    private String treeDesc;

    /**
     * 根节点规则标识。
     */
    private String treeNodeRuleKey;
}
