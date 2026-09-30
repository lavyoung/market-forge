package com.lavyoung.marketforge.infrastructure.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 带业务名称的线程工厂。
 * <p>
 * 为线程池创建带有统一项目前缀和业务池名称的线程，方便日志、线程转储和监控定位异步任务来源。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/29
 */
public class NamedThreadFactory implements ThreadFactory {

    private final String namePrefix;

    private final AtomicInteger index = new AtomicInteger(1);

    /**
     * 创建命名线程工厂。
     *
     * @param poolName 业务线程池名称
     */
    public NamedThreadFactory(String poolName) {
        this.namePrefix = "market-forge-" + poolName + "-";
    }

    /**
     * 创建一个非守护业务线程。
     *
     * @param runnable 待执行任务
     * @return 带统一命名前缀的线程
     */
    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable, namePrefix + index.incrementAndGet());
        thread.setDaemon(false);
        return thread;
    }
}
