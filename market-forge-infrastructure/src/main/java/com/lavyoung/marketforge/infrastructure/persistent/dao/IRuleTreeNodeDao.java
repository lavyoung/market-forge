package com.lavyoung.marketforge.infrastructure.persistent.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.RuleTreeNodePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 规则树节点数据访问接口。
 * <p>
 * 通过 MyBatis-Plus 提供规则树节点配置的通用持久化能力。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/07
 */
@Mapper
public interface IRuleTreeNodeDao extends BaseMapper<RuleTreeNodePO> {

    /**
     * 查询指定规则树的全部节点。
     *
     * @param treeId 规则树标识
     * @return 节点列表；不存在节点时返回空列表
     */
    @Select("""
            SELECT id, tree_id, rule_key, rule_desc, rule_value,
                   create_time, update_time
            FROM rule_tree_node
            WHERE tree_id = #{treeId}
            ORDER BY id
            """)
    List<RuleTreeNodePO> queryByTreeId(@Param("treeId") String treeId);
}
