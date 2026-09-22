package com.lavyoung.marketforge.infrastructure.persistent.dao.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityAccountDayPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 用户活动日账户数据访问接口。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Mapper
public interface IActivityAccountDayDao extends BaseMapper<ActivityAccountDayPO> {

    /**
     * 按用户、活动和日期查询用户活动日账户。
     *
     * @param userId     用户标识
     * @param activityId 活动标识
     * @param day        账户归属日期
     * @return 匹配的用户活动日账户；不存在时返回 {@link Optional#empty()}
     */
    Optional<ActivityAccountDayPO> queryByUserIdAndActivityIdAndDay(
            @Param("userId") String userId,
            @Param("activityId") Long activityId,
            @Param("day") LocalDate day);

    /**
     * 原子扣减用户活动日账户剩余额度。
     *
     * @param userId     用户标识
     * @param activityId 活动标识
     * @param day        账户归属日期
     * @return 成功扣减时为 {@code 1}，额度不足或账户不存在时为 {@code 0}
     */
    int decrementDayCountSurplus(
            @Param("userId") String userId,
            @Param("activityId") Long activityId,
            @Param("day") LocalDate day);
}
