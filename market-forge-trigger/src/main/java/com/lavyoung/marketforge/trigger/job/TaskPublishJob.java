package com.lavyoung.marketforge.trigger.job;

import com.lavyoung.marketforge.application.message.TaskPublishApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 本地消息任务补偿调度器。
 * <p>
 * 周期性扫描待投递、到期重试和租约超时的消息任务，补偿事务提交后即时投递未能执行的窗口。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
public class TaskPublishJob {

    private static final int DEFAULT_BATCH_SIZE = 100;

    private final TaskPublishApplicationService taskPublishApplicationService;

    /**
     * 触发一批本地消息任务的异步投递。
     */
    @Scheduled(fixedDelayString = "${market-forge.job.message-task.fixed-delay-ms:5000}")
    public void exec() {
        taskPublishApplicationService.publishPendingTasks(DEFAULT_BATCH_SIZE);
    }
}
