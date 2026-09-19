package com.lavyoung.marketforge.infrastructure.persistent.dao.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityCountPO;
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
public interface IActivityCountDao extends BaseMapper<ActivityCountPO> {

    /**
     * 根据活动次数配置标识查询配置。
     *
     * @param activityCountId 活动次数配置标识
     * @return 次数配置持久化对象；不存在时返回 {@link Optional#empty()}
     */
    Optional<ActivityCountPO> queryByActivityCountId(@Param("activityCountId") Long activityCountId);
}
