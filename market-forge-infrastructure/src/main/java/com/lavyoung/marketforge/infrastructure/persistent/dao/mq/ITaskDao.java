package com.lavyoung.marketforge.infrastructure.persistent.dao.mq;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.model.TaskPublishingClaim;
import com.lavyoung.marketforge.infrastructure.persistent.po.TaskPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 消息任务数据访问接口。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Mapper
public interface ITaskDao extends BaseMapper<TaskPO> {

    /**
     * 按事件唯一标识查询消息任务。
     *
     * @param userId  用户标识，同时作为任务表分片键
     * @param eventId 事件唯一标识
     * @return 匹配的消息任务；不存在时返回 {@link Optional#empty()}
     */
    Optional<TaskPO> queryByEventId(@Param("userId") String userId, @Param("eventId") String eventId);

    /**
     * 按任务当前状态和投递租约原子抢占投递权。
     *
     * <p>该更新使用数据库条件更新保证并发安全。当多个线程同时抢占同一任务时，
     * 只有一个线程能够更新成功并获得投递权。</p>
     *
     * @param claim 投递权抢占参数
     * @return 更新成功的记录数；{@code 1} 表示抢占成功，{@code 0} 表示任务不可抢占
     */
    int markPublishing(@Param("claim") TaskPublishingClaim claim);
}
