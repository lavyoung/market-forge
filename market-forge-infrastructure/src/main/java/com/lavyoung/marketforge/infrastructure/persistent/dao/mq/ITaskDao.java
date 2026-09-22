package com.lavyoung.marketforge.infrastructure.persistent.dao.mq;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lavyoung.marketforge.infrastructure.persistent.po.mq.TaskPO;
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
     * @param eventId 事件唯一标识
     * @return 匹配的消息任务；不存在时返回 {@link Optional#empty()}
     */
    Optional<TaskPO> queryByEventId(@Param("eventId") String eventId);
}
