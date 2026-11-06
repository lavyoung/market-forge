package com.lavyoung.marketforge.infrastructure.persistent.dao.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.model.ActivityStagePublicationTransitionParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityStagePublicationPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 活动展台发布记录数据访问接口。
 *
 * <p>普通新增使用 {@link BaseMapper#insert(Object)}，查询和并发条件迁移由 XML Mapper 提供。
 * DAO 只表达数据库操作，不承担发布状态机规则。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/06
 */
@Mapper
public interface IActivityStagePublicationDao extends BaseMapper<ActivityStagePublicationPO> {

    /**
     * 查询指定展台版本的发布记录。
     *
     * @param stageId      展台业务标识
     * @param stageVersion 展台乐观锁版本
     * @return 匹配的发布记录；尚未发布时返回空
     */
    Optional<ActivityStagePublicationPO> queryByStageAndVersion(@Param("stageId") Long stageId,
                                                                @Param("stageVersion") int stageVersion);

    /**
     * 使用原状态快照作为条件更新发布进度。
     *
     * @param transition 发布记录迁移参数
     * @return 受影响的记录数；返回零表示原快照已经过期
     */
    int transition(ActivityStagePublicationTransitionParam transition);
}
