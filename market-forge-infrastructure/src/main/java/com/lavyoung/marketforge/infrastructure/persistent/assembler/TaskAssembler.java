package com.lavyoung.marketforge.infrastructure.persistent.assembler;

import com.lavyoung.marketforge.domain.messaging.model.entity.TaskEntity;
import com.lavyoung.marketforge.infrastructure.persistent.po.mq.TaskPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 消息任务持久化对象与领域实体转换器。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface TaskAssembler {

    /**
     * 将消息任务持久化对象转换为领域实体。
     *
     * @param source 消息任务持久化对象
     * @return 消息任务领域实体
     */
    TaskEntity toEntity(TaskPO source);

    /**
     * 批量将消息任务持久化对象转换为领域实体。
     *
     * @param sources 消息任务持久化对象列表
     * @return 消息任务领域实体列表
     */
    List<TaskEntity> toEntities(List<TaskPO> sources);

    /**
     * 将消息任务领域实体转换为待新增的持久化对象。
     *
     * @param source 消息任务领域实体
     * @return 忽略数据库生成字段后的消息任务持久化对象
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    TaskPO toPO(TaskEntity source);

    /**
     * 批量将消息任务领域实体转换为待新增的持久化对象。
     *
     * @param sources 消息任务领域实体列表
     * @return 忽略数据库生成字段后的消息任务持久化对象列表
     */
    List<TaskPO> toPOs(List<TaskEntity> sources);
}
