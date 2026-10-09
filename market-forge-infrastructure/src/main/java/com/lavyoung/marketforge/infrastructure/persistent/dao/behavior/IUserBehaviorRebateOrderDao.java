package com.lavyoung.marketforge.infrastructure.persistent.dao.behavior;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.behavior.UserBehaviorRebateOrderPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户行为返利订单数据访问接口。
 *
 * <p>负责 {@code user_behavior_rebate_order} 表的新增和查询。
 * 当前章节创建订单时主要使用 MyBatis-Plus 的 {@link BaseMapper#insert(Object)}。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/08
 */
@Mapper
public interface IUserBehaviorRebateOrderDao extends BaseMapper<UserBehaviorRebateOrderPO> {


}
