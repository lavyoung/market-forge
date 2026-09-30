package com.lavyoung.marketforge.infrastructure.concurrent;

import java.util.concurrent.*;

/**
 * 自动传递 MDC 上下文的线程池执行器。
 * <p>
 * 提交任务时统一包裹 {@link MdcTaskDecorator}，确保异步任务日志仍能携带请求链路标识。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/29
 */
public class MdcThreadPoolExecutor extends ThreadPoolExecutor {

    /**
     * 创建具备 MDC 透传能力的线程池。
     *
     * @param corePoolSize    核心线程数
     * @param maximumPoolSize 最大线程数
     * @param keepAliveTime   空闲线程存活时间
     * @param unit            存活时间单位
     * @param workQueue       任务队列
     * @param threadFactory   线程工厂
     * @param handler         拒绝策略
     */
    public MdcThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    /**
     * 提交任务并在执行前恢复提交线程的 MDC 上下文。
     *
     * @param command 待执行任务
     */
    @Override
    public void execute(Runnable command) {
        super.execute(MdcTaskDecorator.decorate(command));
    }
}
