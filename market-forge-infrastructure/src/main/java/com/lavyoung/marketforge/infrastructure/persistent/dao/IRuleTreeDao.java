package com.lavyoung.marketforge.infrastructure.persistent.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.RuleTreePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/**
 * 规则树主表数据访问接口。
 * <p>
 * 通过 MyBatis-Plus 提供规则树基础配置的通用持久化能力。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/07
 */
@Mapper
public interface IRuleTreeDao extends BaseMapper<RuleTreePO> {

    /**
     * 根据根节点规则标识查询一棵规则树。
     *
     * @param rootRuleKey 根节点规则标识
     * @return 匹配的规则树；不存在时返回空
     */
    @Select("""
            SELECT id, tree_id, tree_desc, tree_node_rule_key,
                   create_time, update_time
            FROM rule_tree
            WHERE tree_node_rule_key = #{rootRuleKey}
            LIMIT 1
            """)
    Optional<RuleTreePO> queryByRootRuleKey(@Param("rootRuleKey") String rootRuleKey);
}
