package com.lavyoung.marketforge.infrastructure.persistent.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.RuleTreePO;
import org.apache.ibatis.annotations.Mapper;

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
}
