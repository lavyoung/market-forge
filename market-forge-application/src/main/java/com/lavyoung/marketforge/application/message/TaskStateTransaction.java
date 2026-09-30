package com.lavyoung.marketforge.application.message;

import com.lavyoung.marketforge.domain.message.model.entity.TaskEntity;
import com.lavyoung.marketforge.domain.message.repository.ITaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 本地消息任务状态变更的短事务边界。
 * <p>
 * 消息发布属于外部网络调用，不应占用数据库事务；本组件只封装任务抢占、
 * 发布成功和发布失败三类状态变更，确保分片表写操作满足 LavShard 的事务要求。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
public class TaskStateTransaction {

    private final ITaskRepository taskRepository;

    /**
     * 查询并以 CAS 方式抢占一条可投递任务。
     *
     * @param userId                  用户标识，同时作为任务表分片键
     * @param eventId                 事件标识
     * @param now                     当前时间
     * @param publishingExpiredBefore 投递租约失效边界
     * @return 抢占成功的任务；任务不存在或已被其他线程抢占时返回空
     */
    @Transactional(rollbackFor = Exception.class)
    public Optional<TaskEntity> claim(String userId, String eventId, LocalDateTime now,
                                      LocalDateTime publishingExpiredBefore) {
        Optional<TaskEntity> task = taskRepository.queryByEventId(userId, eventId);
        if (task.isEmpty()) {
            return Optional.empty();
        }
        return taskRepository.markPublishing(userId, eventId, now, publishingExpiredBefore)
                ? task
                : Optional.empty();
    }

    /**
     * 将已抢占任务标记为发布成功。
     *
     * @param userId  用户标识，同时作为任务表分片键
     * @param eventId 事件标识
     * @return 状态更新成功时返回 {@code true}
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean markPublished(String userId, String eventId) {
        return taskRepository.markPublished(userId, eventId);
    }

    /**
     * 记录任务发布失败原因和下一次重试时间。
     *
     * @param userId        用户标识，同时作为任务表分片键
     * @param eventId       事件标识
     * @param errorMessage  发布失败原因
     * @param nextRetryTime 下一次允许重试时间
     * @return 状态更新成功时返回 {@code true}
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean markPublishFailed(String userId, String eventId, String errorMessage,
                                     LocalDateTime nextRetryTime) {
        return taskRepository.markPublishFailed(userId, eventId, errorMessage, nextRetryTime);
    }
}
