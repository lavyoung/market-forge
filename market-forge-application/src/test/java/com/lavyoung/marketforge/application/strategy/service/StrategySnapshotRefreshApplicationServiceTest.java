package com.lavyoung.marketforge.application.strategy.service;

import com.lavyoung.marketforge.application.strategy.service.impl.StrategySnapshotRefreshApplicationServiceImpl;
import com.lavyoung.marketforge.domain.strategy.service.armorcy.IStrategyArmory;
import com.lavyoung.marketforge.types.concurrent.NamedExecutorProvider;
import com.lavyoung.marketforge.types.concurrent.ThreadPoolNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 验证策略快照刷新请求在应用层的异步调度、同策略合并和异常恢复行为。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class StrategySnapshotRefreshApplicationServiceTest {

    private static final Long STRATEGY_ID = 100_001L;

    @Mock
    private IStrategyArmory strategyArmory;

    @Mock
    private NamedExecutorProvider executorProvider;

    @Mock
    private ExecutorService executorService;

    private List<Runnable> submittedTasks;
    private StrategySnapshotRefreshApplicationServiceImpl refreshApplicationService;

    /**
     * Given 可控的策略装配器和命名执行器，When 初始化服务，Then 后续用例可以手动驱动异步任务。
     */
    @BeforeEach
    void setUp() {
        submittedTasks = new ArrayList<>();
        lenient().when(executorProvider.getExecutor(ThreadPoolNames.STRATEGY_REFRESH)).thenReturn(executorService);
        lenient().doAnswer(invocation -> {
            submittedTasks.add(invocation.getArgument(0));
            return null;
        }).when(executorService).execute(any(Runnable.class));
        refreshApplicationService = new StrategySnapshotRefreshApplicationServiceImpl(strategyArmory, executorProvider);
    }

    /**
     * Given 同一策略正在刷新，When 短时间再次提交多个请求，Then 请求合并为一次后续重建且不重复提交线程任务。
     */
    @Test
    void shouldMergeRepeatedRequestsIntoOneFollowUpRefresh() {
        // Given
        AtomicInteger assemblyCount = new AtomicInteger();
        doAnswer(invocation -> {
            if (assemblyCount.incrementAndGet() == 1) {
                assertFalse(refreshApplicationService.requestRefresh(STRATEGY_ID));
                assertFalse(refreshApplicationService.requestRefresh(STRATEGY_ID));
            }
            return true;
        }).when(strategyArmory).assembleLotteryStrategy(STRATEGY_ID);

        // When
        boolean scheduled = refreshApplicationService.requestRefresh(STRATEGY_ID);
        boolean mergedBeforeExecution = refreshApplicationService.requestRefresh(STRATEGY_ID);
        submittedTasks.get(0).run();

        // Then
        assertTrue(scheduled);
        assertFalse(mergedBeforeExecution);
        assertEquals(1, submittedTasks.size());
        assertEquals(2, assemblyCount.get());
        verify(strategyArmory, org.mockito.Mockito.times(2)).assembleLotteryStrategy(STRATEGY_ID);

        // And：任务稳定退出后，同一策略可以再次创建新的刷新任务。
        assertTrue(refreshApplicationService.requestRefresh(STRATEGY_ID));
        assertEquals(2, submittedTasks.size());
    }

    /**
     * Given 两个不同策略都没有刷新任务，When 分别提交刷新请求，Then 各自创建独立任务且互不合并。
     */
    @Test
    void shouldScheduleIndependentTasksForDifferentStrategies() {
        // Given
        Long anotherStrategyId = 100_002L;

        // When
        boolean firstScheduled = refreshApplicationService.requestRefresh(STRATEGY_ID);
        boolean secondScheduled = refreshApplicationService.requestRefresh(anotherStrategyId);

        // Then
        assertTrue(firstScheduled);
        assertTrue(secondScheduled);
        assertEquals(2, submittedTasks.size());

        // And：分别执行两个任务时，只装配各自对应的策略。
        submittedTasks.forEach(Runnable::run);
        verify(strategyArmory).assembleLotteryStrategy(STRATEGY_ID);
        verify(strategyArmory).assembleLotteryStrategy(anotherStrategyId);
    }

    /**
     * Given 策略重建发生业务异常，When 当前工作任务结束，Then 清理运行状态并允许后续请求重新调度。
     */
    @Test
    void shouldAllowRetryAfterAssemblyFailure() {
        // Given
        doThrow(new IllegalStateException("database unavailable"))
                .when(strategyArmory).assembleLotteryStrategy(STRATEGY_ID);

        // When
        assertTrue(refreshApplicationService.requestRefresh(STRATEGY_ID));
        submittedTasks.get(0).run();

        // Then
        assertTrue(refreshApplicationService.requestRefresh(STRATEGY_ID));
        assertEquals(2, submittedTasks.size());
    }

    /**
     * Given 专用线程池拒绝接收任务，When 提交刷新请求，Then 向调用方暴露拒绝异常并回滚策略运行状态。
     */
    @Test
    void shouldRollbackRegistrationWhenExecutorRejectsTask() {
        // Given
        RejectedExecutionException rejection = new RejectedExecutionException("queue full");
        reset(executorService);
        doThrow(rejection).when(executorService).execute(any(Runnable.class));

        // When
        RejectedExecutionException exception = assertThrows(RejectedExecutionException.class,
                () -> refreshApplicationService.requestRefresh(STRATEGY_ID));

        // Then
        assertEquals(rejection, exception);

        // And：回滚后恢复执行器，下一次请求仍然可以正常调度。
        doAnswer(invocation -> {
            submittedTasks.add(invocation.getArgument(0));
            return null;
        }).when(executorService).execute(any(Runnable.class));
        assertTrue(refreshApplicationService.requestRefresh(STRATEGY_ID));
        assertEquals(1, submittedTasks.size());
    }

    /**
     * Given 缺少策略标识，When 请求异步刷新，Then 在访问线程池和领域服务前拒绝调用。
     */
    @Test
    void shouldRejectNullStrategyId() {
        assertThrows(NullPointerException.class, () -> refreshApplicationService.requestRefresh(null));
    }
}
