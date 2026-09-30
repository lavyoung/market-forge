package com.lavyoung.marketforge.application.message;

import com.lavyoung.marketforge.domain.message.model.entity.TaskEntity;
import com.lavyoung.marketforge.domain.message.repository.ITaskRepository;
import com.lavyoung.marketforge.types.concurrent.NamedExecutorProvider;
import com.lavyoung.marketforge.types.concurrent.ThreadPoolNames;
import com.lavyoung.marketforge.types.messaging.IntegrationEvent;
import com.lavyoung.marketforge.types.messaging.IntegrationEventCodec;
import com.lavyoung.marketforge.types.messaging.MessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 本地消息任务投递应用服务。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/30
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskPublishApplicationService {

    private static final Duration PUBLISHING_LEASE = Duration.ofMinutes(1);
    private static final Duration MAX_RETRY_DELAY = Duration.ofMinutes(5);

    private final ITaskRepository taskRepository;
    private final TaskStateTransaction taskStateTransaction;
    private final MessagePublisher messagePublisher;
    private final IntegrationEventCodec integrationEventCodec;
    private final NamedExecutorProvider executorProvider;

    /**
     * 异步投递指定事件的本地消息任务。
     *
     * @param userId  用户标识，同时作为任务表分片键
     * @param eventId 事件标识
     */
    public void publishAsync(String userId, String eventId) {
        executorProvider.getExecutor(ThreadPoolNames.MESSAGE_TASK)
                .execute(() -> publishOne(userId, eventId));
    }

    /**
     * 批量异步投递待处理消息任务。
     *
     * @param limit 本次最多投递条数
     */
    public void publishPendingTasks(int limit) {
        LocalDateTime now = LocalDateTime.now();
        List<TaskEntity> tasks = taskRepository.queryPublishableTasks(
                limit, now, now.minus(PUBLISHING_LEASE));
        tasks.forEach(task -> publishAsync(task.userId(), task.eventId()));
    }

    /**
     * 投递单条本地消息任务。
     *
     * @param userId  用户标识，同时作为任务表分片键
     * @param eventId 事件标识
     */
    public void publishOne(String userId, String eventId) {
        LocalDateTime now = LocalDateTime.now();
        TaskEntity taskEntity = taskStateTransaction.claim(
                userId, eventId, now, now.minus(PUBLISHING_LEASE)).orElse(null);
        if (taskEntity == null) {
            log.debug("本地消息任务不存在、尚未到重试时间或已被其他线程处理 eventId={}", eventId);
            return;
        }

        try {
            IntegrationEvent event = integrationEventCodec.deserialize(taskEntity.eventType(), taskEntity.messageBody());
            messagePublisher.publish(event);
            if (!taskStateTransaction.markPublished(userId, eventId)) {
                log.error("本地消息任务投递成功但状态更新失败，后续可能重复投递 eventId={}", eventId);
            }
        } catch (Exception exception) {
            LocalDateTime nextRetryTime = LocalDateTime.now().plus(retryDelay(taskEntity.retryCount()));
            try {
                taskStateTransaction.markPublishFailed(userId, eventId, exception.getMessage(), nextRetryTime);
            } catch (Exception stateException) {
                exception.addSuppressed(stateException);
                log.error("记录本地消息投递失败状态时发生异常 eventId={}", eventId, stateException);
            }
            log.warn("本地消息任务投递失败 eventId={}", eventId, exception);
        }
    }

    private Duration retryDelay(Integer retryCount) {
        int safeRetryCount = retryCount == null ? 0 : Math.max(retryCount, 0);
        long delaySeconds = 1L << Math.min(safeRetryCount, 8);
        return Duration.ofSeconds(Math.min(delaySeconds, MAX_RETRY_DELAY.toSeconds()));
    }
}
