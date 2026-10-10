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

    /**
     * 原子累加用户活动日账户额度。
     *
     * <p>SKU 返利入账时，如果用户当日账户已经存在，需要同时增加当日总额度和当日剩余额度。
     * 该方法用一条 SQL 完成累加，避免先查再改导致并发入账时丢失次数。</p>
     *
     * @param userId     用户标识
     * @param activityId 活动标识
     * @param day        账户归属日期
     * @param dayCount   本次需要增加的日额度
     * @return 成功累加时为 {@code 1}，账户不存在时为 {@code 0}
     */
    int incrementDayCount(
            @Param("userId") String userId,
            @Param("activityId") Long activityId,
            @Param("day") LocalDate day,
            @Param("dayCount") Integer dayCount);
}
