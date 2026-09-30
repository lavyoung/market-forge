package com.lavyoung.marketforge.domain.message.repository;

import com.lavyoung.marketforge.domain.message.model.entity.TaskEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 本地消息任务仓储端口。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/29
 */
public interface ITaskRepository {

    /**
     * 保存本地消息任务。
     *
     * @param taskEntity 本地消息任务
     */
    void save(TaskEntity taskEntity);

    /**
     * 按事件标识查询本地消息任务。
     *
     * @param userId  用户标识，同时作为任务表分片键
     * @param eventId 事件标识
     * @return 匹配的消息任务；不存在时返回空
     */
    Optional<TaskEntity> queryByEventId(String userId, String eventId);

    /**
     * 查询待投递消息任务。
     *
     * @param limit                   查询条数
     * @param now                     当前时间，用于判断失败任务是否到达重试时间
     * @param publishingExpiredBefore 投递租约失效边界
     * @return 待投递消息任务列表
     */
    List<TaskEntity> queryPublishableTasks(int limit, LocalDateTime now, LocalDateTime publishingExpiredBefore);

    /**
     * 将消息任务标记为处理中。
     *
     * @param userId                  用户标识，同时作为任务表分片键
     * @param eventId                 事件标识
     * @param now                     当前时间
     * @param publishingExpiredBefore 投递租约失效边界
     * @return 标记成功时返回 {@code true}
     */
    boolean markPublishing(String userId, String eventId, LocalDateTime now, LocalDateTime publishingExpiredBefore);

    /**
     * 将消息任务标记为已完成。
     *
     * @param userId  用户标识，同时作为任务表分片键
     * @param eventId 事件标识
     * @return 标记成功时返回 {@code true}
     */
    boolean markPublished(String userId, String eventId);

    /**
     * 将消息任务标记为失败。
     *
     * @param userId        用户标识，同时作为任务表分片键
     * @param eventId       事件标识
     * @param errorMessage  失败原因
     * @param nextRetryTime 下一次允许重试时间
     * @return 标记成功时返回 {@code true}
     */
    boolean markPublishFailed(String userId, String eventId, String errorMessage, LocalDateTime nextRetryTime);
}
