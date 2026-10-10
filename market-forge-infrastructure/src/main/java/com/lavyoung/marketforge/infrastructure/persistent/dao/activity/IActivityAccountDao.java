package com.lavyoung.marketforge.infrastructure.persistent.dao.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityAccountPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 用户活动总账户数据访问接口。
 * <p>
 * 提供用户在指定活动下的总额度账户查询与剩余额度原子扣减能力。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Mapper
public interface IActivityAccountDao extends BaseMapper<ActivityAccountPO> {

    /**
     * 查询用户在指定活动下的账户。
     *
     * @param userId     用户标识
     * @param activityId 活动标识
     * @return 活动账户持久化对象；不存在时返回 {@link Optional#empty()}
     */
    Optional<ActivityAccountPO> queryByUserIdAndActivityId(
            @Param("userId") String userId,
            @Param("activityId") Long activityId);

    /**
     * 原子扣减用户活动总账户剩余额度。
     *
     * @param userId     用户标识
     * @param activityId 活动标识
     * @return 成功扣减时为 {@code 1}，额度不足或账户不存在时为 {@code 0}
     */
    int decrementTotalCountSurplus(
            @Param("userId") String userId,
            @Param("activityId") Long activityId);

    /**
     * 原子累加用户活动总账户额度。
     *
     * <p>SKU 返利入账时，如果用户活动总账户已经存在，需要同时增加总额度、日额度、月额度
     * 以及对应剩余额度。该方法用一条 SQL 完成累加，避免并发入账时发生次数覆盖。</p>
     *
     * @param userId     用户标识
     * @param activityId 活动标识
     * @param totalCount 本次需要增加的总额度
     * @param dayCount   本次需要增加的日额度
     * @param monthCount 本次需要增加的月额度
     * @return 成功累加时为 {@code 1}，账户不存在时为 {@code 0}
     */
    int incrementTotalCount(
            @Param("userId") String userId,
            @Param("activityId") Long activityId,
            @Param("totalCount") Integer totalCount,
            @Param("dayCount") Integer dayCount,
            @Param("monthCount") Integer monthCount);
}
