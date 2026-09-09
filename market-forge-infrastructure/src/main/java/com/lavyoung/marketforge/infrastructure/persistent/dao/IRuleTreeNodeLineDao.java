package com.lavyoung.marketforge.infrastructure.persistent.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.RuleTreeNodeLinePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 规则树节点连线数据访问接口。
 * <p>
 * 通过 MyBatis-Plus 提供节点流转条件的通用持久化能力。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/07
 */
@Mapper
public interface IRuleTreeNodeLineDao extends BaseMapper<RuleTreeNodeLinePO> {

    /**
     * 查询指定规则树的全部节点连线。
     *
     * @param treeId 规则树标识
     * @return 连线列表；不存在连线时返回空列表
     */
    @Select("""
            SELECT id, tree_id, rule_node_from, rule_node_to,
                   rule_limit_type, rule_limit_value, create_time, update_time
            FROM rule_tree_node_line
            WHERE tree_id = #{treeId}
            ORDER BY id
            """)
    List<RuleTreeNodeLinePO> queryByTreeId(@Param("treeId") String treeId);
}
