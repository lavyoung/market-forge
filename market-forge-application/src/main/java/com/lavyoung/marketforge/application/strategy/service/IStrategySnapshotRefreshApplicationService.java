package com.lavyoung.marketforge.application.strategy.service;

/**
 * 策略概率快照刷新应用服务。
 *
 * <p>负责接收策略配置变更产生的刷新请求，并将同一策略的高频请求合并。
 * 本接口只表达应用用例，不暴露线程池、Redis 或快照存储等技术细节。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
public interface IStrategySnapshotRefreshApplicationService {

    /**
     * 请求异步刷新指定策略的概率快照。
     *
     * <p>若该策略没有正在执行的刷新任务，则创建新任务；若任务已经排队或执行，
     * 仅记录“仍有新配置”状态，由现有任务完成后再执行一次重建。</p>
     *
     * @param strategyId 需要重新装配的策略标识
     * @return 创建新异步任务返回 {@code true}；合并到已有任务返回 {@code false}
     * @throws NullPointerException                            策略标识为空时抛出
     * @throws java.util.concurrent.RejectedExecutionException 专用线程池容量耗尽时抛出
     */
    boolean requestRefresh(Long strategyId);
}
