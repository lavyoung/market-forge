package com.lavyoung.marketforge.infrastructure.persistent.dao.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityPO;
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
public interface IActivityDao extends BaseMapper<ActivityPO> {

    /**
     * 根据活动业务标识查询活动。
     *
     * @param activityId 活动业务标识
     * @return 活动持久化对象；不存在时返回 {@link Optional#empty()}
     */
    Optional<ActivityPO> queryByActivityId(@Param("activityId") Long activityId);
}
