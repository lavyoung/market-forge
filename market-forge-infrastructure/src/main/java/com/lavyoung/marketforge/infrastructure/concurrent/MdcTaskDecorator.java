package com.lavyoung.marketforge.infrastructure.concurrent;

import org.slf4j.MDC;

import java.util.Map;

/**
 * 异步任务 MDC 上下文装饰器。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/29
 */
public final class MdcTaskDecorator {

    private MdcTaskDecorator() {
    }

    /**
     * 包装异步任务并透传当前线程的 MDC 上下文。
     *
     * @param task 原始任务
     * @return 带 MDC 上下文恢复和清理逻辑的任务
     */
    public static Runnable decorate(Runnable task) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            try {
                if (contextMap == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(contextMap);
                }
                task.run();
            } finally {
                if (previous == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(previous);
                }
            }
        };
    }

}
