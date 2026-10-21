package com.lavyoung.marketforge.application.strategy.service.impl;

import com.lavyoung.marketforge.application.strategy.service.IStrategySnapshotRefreshApplicationService;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyArmory;
import com.lavyoung.marketforge.types.concurrent.NamedExecutorProvider;
import com.lavyoung.marketforge.types.concurrent.ThreadPoolNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 合并同一策略高频变更请求的快照刷新应用服务。
 *
 * <p>映射中只保存正在排队或执行的策略。刷新稳定结束后立即删除状态，
 * 因而状态数量受专用线程池工作线程和有界队列共同限制，不会永久积累。</p>
 *
 * <p>执行中的新请求只设置待刷新标记。当前重建完成后若发现该标记，
 * 再读取一次数据库最新配置；无论期间收到多少请求，都只追加一次重建。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategySnapshotRefreshApplicationServiceImpl implements IStrategySnapshotRefreshApplicationService {

    private final IStrategyArmory strategyArmory;
    private final NamedExecutorProvider executorProvider;
    private final ConcurrentMap<Long, RefreshState> refreshStates = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     *
     * <p>任务注册与状态修改通过 {@link ConcurrentMap#compute(Object, java.util.function.BiFunction)}
     * 按策略标识原子执行。线程池必须使用 {@code abort} 拒绝策略，避免重建任务退化为
     * 在配置监听线程中同步执行。</p>
     */
    @Override
    public boolean requestRefresh(Long strategyId) {
        Long validStrategyId = Objects.requireNonNull(strategyId, "strategyId must not be null");
        ExecutorService executorService = executorProvider.getExecutor(ThreadPoolNames.STRATEGY_REFRESH);
        AtomicBoolean scheduled = new AtomicBoolean();
        refreshStates.compute(validStrategyId, (id, currentState) -> {
            if (currentState != null) {
                currentState.markRequested();
                return currentState;
            }

            RefreshState newState = new RefreshState();
            newState.markRequested();
            CompletableFuture.runAsync(() -> drainRefreshRequest(id, newState), executorService);
            scheduled.set(true);

            return newState;
        });
        return scheduled.get();
    }

    /**
     * 持续处理指定策略的刷新请求，直到当前执行期间没有收到新的变更。
     *
     * <p>每轮开始前清除待刷新标记。若重建期间又收到请求，轮次结束时该标记
     * 会重新变为 {@code true}，工作线程随后再执行一轮并读取数据库最新配置。</p>
     *
     * @param strategyId 策略标识
     * @param state      当前异步任务持有的刷新状态
     */
    private void drainRefreshRequest(Long strategyId, RefreshState state) {
        boolean continueRefresh;
        do {
            state.beginCycle();
            try {
                boolean assembled = strategyArmory.assembleLotteryStrategy(strategyId);
                if (!assembled) {
                    log.warn("策略概率快照刷新未完成 strategyId={}", strategyId);
                }
            } catch (RuntimeException e) {
                log.error("策略概率快照刷新失败 strategyId={}", strategyId, e);
            }
            continueRefresh = completeCycle(strategyId, state);
        } while (continueRefresh);
    }

    /**
     * 原子判断当前任务是否还需要执行下一轮，并在稳定后移除临时状态。
     *
     * @param strategyId 策略标识
     * @param state      当前工作线程持有的状态对象
     * @return 执行期间收到过新请求时返回 {@code true}
     */
    private boolean completeCycle(Long strategyId, RefreshState state) {
        AtomicBoolean continueRefresh = new AtomicBoolean();
        refreshStates.compute(strategyId, (id, currentState) -> {
            if (currentState != state) {
                return currentState;
            }
            if (state.hasPendingRequest()) {
                continueRefresh.set(true);
                return state;
            }
            return null;
        });
        return continueRefresh.get();
    }

    /**
     * 单个策略刷新任务的最小可变状态。
     *
     * <p>这里只记录是否存在尚未被当前重建覆盖的新请求，不保存请求数量；
     * 因为策略重建始终读取数据库最新状态，累计次数没有业务意义。</p>
     */
    private static final class RefreshState {

        private final AtomicBoolean pendingRequest = new AtomicBoolean();

        private void markRequested() {
            pendingRequest.set(true);
        }

        private void beginCycle() {
            pendingRequest.set(false);
        }

        private boolean hasPendingRequest() {
            return pendingRequest.get();
        }
    }
}
