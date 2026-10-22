package com.lavyoung.marketforge.infrastructure.persistent.dao.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.model.ActivityDefinitionSearchParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
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

    /**
     * 统计满足运营查询条件的活动数量。
     *
     * @param searchParam 活动过滤条件和分页窗口
     * @return 匹配记录总数
     */
    long countActivityDefinitions(ActivityDefinitionSearchParam searchParam);

    /**
     * 分页查询活动定义。
     *
     * @param searchParam 活动过滤条件和分页窗口
     * @return 按更新时间和主键倒序排列的活动记录
     */
    List<ActivityPO> queryActivityDefinitionPage(ActivityDefinitionSearchParam searchParam);
}
