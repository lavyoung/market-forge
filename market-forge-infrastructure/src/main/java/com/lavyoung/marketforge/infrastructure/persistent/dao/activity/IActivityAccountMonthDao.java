package com.lavyoung.marketforge.infrastructure.persistent.dao.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityAccountMonthPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 用户活动月账户数据访问接口。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Mapper
public interface IActivityAccountMonthDao extends BaseMapper<ActivityAccountMonthPO> {

    /**
     * 按用户、活动和月份查询用户活动月账户。
     *
     * @param userId     用户标识
     * @param activityId 活动标识
     * @param month      yyyy-MM 格式的账户归属月份
     * @return 匹配的用户活动月账户；不存在时返回 {@link Optional#empty()}
     */
    Optional<ActivityAccountMonthPO> queryByUserIdAndActivityIdAndMonth(
            @Param("userId") String userId,
            @Param("activityId") Long activityId,
            @Param("month") String month);

    /**
     * 原子扣减用户活动月账户剩余额度。
     *
     * @param userId     用户标识
     * @param activityId 活动标识
     * @param month      yyyy-MM 格式的账户归属月份
     * @return 成功扣减时为 {@code 1}，额度不足或账户不存在时为 {@code 0}
     */
    int decrementMonthCountSurplus(
            @Param("userId") String userId,
            @Param("activityId") Long activityId,
            @Param("month") String month);
}
