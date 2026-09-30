package com.lavyoung.marketforge.infrastructure.concurrent;

import com.lavyoung.marketforge.types.concurrent.RejectedPolicy;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *
 * 线程池创建工厂。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/29
 */
public final class ThreadPoolFactory {

    private ThreadPoolFactory() {

    }

    public static ThreadPoolExecutor create(String name, ThreadPoolProperties.Pool pool) {
        validate(name, pool);
        return new MdcThreadPoolExecutor(
                pool.getCorePoolSize(),
                pool.getMaximumPoolSize(),
                pool.getKeepAliveSeconds(),
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(pool.getQueueCapacity()),
                new NamedThreadFactory(name),
                rejectedExecutionHandler(pool.getRejectedPolicy())
        );
    }

    private static void validate(String name, ThreadPoolProperties.Pool pool) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("thread pool name must not be blank");
        }
        if (pool.getCorePoolSize() <= 0) {
            throw new IllegalArgumentException("corePoolSize must be positive");
        }
        if (pool.getMaximumPoolSize() < pool.getCorePoolSize()) {
            throw new IllegalArgumentException("maximumPoolSize must be greater than or equal to corePoolSize");
        }
        if (pool.getQueueCapacity() <= 0) {
            throw new IllegalArgumentException("queueCapacity must be positive");
        }
    }

    private static RejectedExecutionHandler rejectedExecutionHandler(RejectedPolicy policy) {
        return switch (policy) {
            case ABORT -> new ThreadPoolExecutor.AbortPolicy();
            case CALLER_RUNS -> new ThreadPoolExecutor.CallerRunsPolicy();
            case DISCARD -> new ThreadPoolExecutor.DiscardPolicy();
            case DISCARD_OLDEST -> new ThreadPoolExecutor.DiscardOldestPolicy();
        };
    }
}
