package com.lavyoung.marketforge.infrastructure.concurrent;

import com.lavyoung.marketforge.types.concurrent.RejectedPolicy;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 线程池创建工厂。
 * <p>
 * 根据统一配置创建携带 MDC 透传能力的 {@link ThreadPoolExecutor}。业务代码只应通过
 * {@link ThreadPoolRegistry} 或 {@code NamedExecutorProvider} 获取线程池，避免直接创建线程。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/29
 */
public final class ThreadPoolFactory {

    private ThreadPoolFactory() {

    }

    /**
     * 按业务名称和配置创建线程池。
     *
     * @param name 线程池业务名称
     * @param pool 线程池参数配置
     * @return 可透传 MDC 的线程池执行器
     * @throws IllegalArgumentException 线程池名称为空、核心参数非法或拒绝策略缺失时抛出
     */
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

    /**
     * 将配置枚举转换为 JDK 拒绝策略实现。
     *
     * @param policy 拒绝策略配置
     * @return JDK 线程池拒绝策略
     * @throws NullPointerException 拒绝策略为空时抛出
     */
    private static RejectedExecutionHandler rejectedExecutionHandler(RejectedPolicy policy) {
        return switch (policy) {
            case ABORT -> new ThreadPoolExecutor.AbortPolicy();
            case CALLER_RUNS -> new ThreadPoolExecutor.CallerRunsPolicy();
            case DISCARD -> new ThreadPoolExecutor.DiscardPolicy();
            case DISCARD_OLDEST -> new ThreadPoolExecutor.DiscardOldestPolicy();
        };
    }
}
