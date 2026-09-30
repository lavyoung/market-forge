package com.lavyoung.marketforge.application.message;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 本地消息任务创建事件监听器。
 * <p>
 * 事务提交后立即触发一次异步投递，定时补偿任务再兜底处理未完成消息。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/30
 */
@Component
@RequiredArgsConstructor
public class TaskCreatedEventListener {

    private final TaskPublishApplicationService taskPublishApplicationService;

    /**
     * 在业务事务提交后触发消息投递。
     *
     * @param event 本地消息任务创建事件
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskCreated(TaskCreatedEvent event) {
        taskPublishApplicationService.publishAsync(event.userId(), event.eventId());
    }
}
