package com.lavyoung.marketforge.infrastructure.persistent.dao.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.model.ActivityCountDefinitionSearchParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityCountPO;
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
public interface IActivityCountDao extends BaseMapper<ActivityCountPO> {

    /**
     * 统计满足运营查询条件的活动次数配置数量。
     *
     * @param searchParam 查询条件和分页窗口
     * @return 匹配记录总数
     */
    long countActivityCountDefinitions(ActivityCountDefinitionSearchParam searchParam);

    /**
     * 分页查询活动次数配置。
     *
     * @param searchParam 查询条件和分页窗口
     * @return 按更新时间和主键倒序排列的配置记录
     */
    List<ActivityCountPO> queryActivityCountDefinitionPage(ActivityCountDefinitionSearchParam searchParam);

    /**
     * 根据活动次数配置标识查询配置。
     *
     * @param activityCountId 活动次数配置标识
     * @return 次数配置持久化对象；不存在时返回 {@link Optional#empty()}
     */
    Optional<ActivityCountPO> queryByActivityCountId(@Param("activityCountId") Long activityCountId);

    /**
     * 使用排他锁查询活动次数配置。
     *
     * @param activityCountId 活动次数配置业务标识
     * @return 已锁定的持久化对象；不存在时返回空
     */
    Optional<ActivityCountPO> lockByActivityCountId(@Param("activityCountId") Long activityCountId);

    /**
     * 判断次数配置是否被开放活动引用。
     *
     * @param activityCountId 活动次数配置业务标识
     * @return 存在开放活动引用时返回 {@code true}
     */
    boolean existsOpenActivityReference(@Param("activityCountId") Long activityCountId);

    /**
     * 按版本更新未被开放活动引用的次数配置。
     *
     * @param definition 最新持久化对象
     * @return 影响行数
     */
    int updateIfNotReferencedByOpenActivity(ActivityCountPO definition);
}
