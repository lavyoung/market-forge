package com.lavyoung.marketforge.infrastructure.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * 带业务名称的线程工厂
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/29
 */
public class NamedThreadFactory implements ThreadFactory {

    private final String namePrefix;

    private final AtomicInteger index = new AtomicInteger(1);

    public NamedThreadFactory(String poolName) {
        this.namePrefix = "market-forge-" + poolName + "-";
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable, namePrefix + index.incrementAndGet());
        thread.setDaemon(false);
        return thread;
    }
}
