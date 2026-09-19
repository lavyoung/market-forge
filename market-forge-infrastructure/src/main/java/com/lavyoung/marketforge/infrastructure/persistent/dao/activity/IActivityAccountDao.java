package com.lavyoung.marketforge.infrastructure.persistent.dao.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityAccountPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 *
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
}
