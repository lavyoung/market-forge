package com.lavyoung.marketforge.infrastructure.persistent.dao.behavior;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.behavior.UserBehaviorRebateOrderPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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


    /**
     * 判断指定用户、行为类型和外部业务幂等号下是否存在返利订单。
     *
     * <p>该方法用于查询“今天是否已签到”等状态类接口。
     * 查询条件必须包含 {@code userId}，因为该表按用户标识分库，缺少分片键会导致路由不明确。</p>
     *
     * @param userId        用户标识，同时作为分库路由键
     * @param behaviorType  行为类型编码，例如 {@code sign}
     * @param outBusinessNo 外部业务幂等号，例如 {@code sign-20261010-user-001}
     * @return 存在匹配订单返回 {@code true}，否则返回 {@code false}
     */
    boolean existsByUserIdAndBehaviorTypeAndOutBusinessNo(
            @Param("userId") String userId,
            @Param("behaviorType") String behaviorType,
            @Param("outBusinessNo") String outBusinessNo);
}
