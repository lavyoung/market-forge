package com.lavyoung.marketforge.types.concurrent;

/**
 *
 * 线程池拒绝策略
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/29
 */
public enum RejectedPolicy {
    /**
     * 直接抛出异常，适合关键任务。
     */
    ABORT,

    /**
     * 由提交任务的线程执行，适合削峰。
     */
    CALLER_RUNS,

    /**
     * 直接丢弃新任务。
     */
    DISCARD,

    /**
     * 丢弃队列中最旧的任务。
     */
    DISCARD_OLDEST

}
