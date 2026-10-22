package com.lavyoung.marketforge.infrastructure.persistent.dao.activity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.model.ActivityStageSearchParam;
import com.lavyoung.marketforge.infrastructure.persistent.model.ActivityStageStateUpdateParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityStagePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 活动展台数据访问接口。
 *
 * <p>提供业务标识查询、当前生效配置查询、渠道行锁和带状态版本条件的更新能力。
 * 普通新增继续使用 MyBatis-Plus 的 {@link BaseMapper#insert(Object)}。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Mapper
public interface IActivityStageDao extends BaseMapper<ActivityStagePO> {

    /**
     * 统计满足运营过滤条件的展台数量。
     *
     * @param searchParam 查询和分页参数
     * @return 满足过滤条件的总记录数
     */
    long countActivityStages(ActivityStageSearchParam searchParam);

    /**
     * 分页查询活动展台。
     *
     * @param searchParam 查询和分页参数
     * @return 按更新时间和主键倒序排列的当前页记录
     */
    List<ActivityStagePO> queryActivityStagePage(ActivityStageSearchParam searchParam);

    /**
     * 根据展台业务标识查询配置。
     *
     * @param stageId 展台业务标识
     * @return 匹配的展台记录；不存在时返回空
     */
    Optional<ActivityStagePO> queryByStageId(@Param("stageId") Long stageId);

    /**
     * 查询指定渠道和来源的全部生效配置。
     *
     * @param channel 投放渠道
     * @param source  流量来源
     * @return 生效配置集合；正常情况下最多一条
     */
    List<ActivityStagePO> queryActive(@Param("channel") String channel, @Param("source") String source);

    /**
     * 使用排他锁读取指定渠道和来源的全部展台记录。
     *
     * @param channel 投放渠道
     * @param source  流量来源
     * @return 按数据库主键升序排列的已锁定记录
     */
    List<ActivityStagePO> lockByChannelAndSource(@Param("channel") String channel, @Param("source") String source);

    /**
     * 使用待上架状态和乐观锁版本修改展台配置。
     *
     * @param stage 最新展台持久化对象
     * @return 受影响的记录数
     */
    int updateDraft(ActivityStagePO stage);

    /**
     * 使用当前状态和版本条件推进展台状态。
     *
     * @param stateUpdate 状态条件更新参数
     * @return 受影响的记录数
     */
    int changeState(ActivityStageStateUpdateParam stateUpdate);
}
