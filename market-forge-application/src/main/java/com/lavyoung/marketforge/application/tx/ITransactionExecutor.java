package com.lavyoung.marketforge.application.tx;

import java.util.function.Supplier;

/**
 * 应用层事务执行器。
 * <p>
 * 用于为 application 层的跨聚合写操作提供统一事务边界。调用方应将已经完成领域计算、
 * 参数校验和外部查询后的数据库写入逻辑放入事务回调中，避免把耗时的远程调用或随机计算
 * 包进数据库事务。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public interface ITransactionExecutor {

    /**
     * 在事务中执行无返回值操作。
     *
     * @param action 需要事务保护的写操作
     * @throws NullPointerException action 为 {@code null} 时抛出
     * @throws RuntimeException     事务回调执行失败时抛出，并触发事务回滚
     */
    void execute(Runnable action);

    /**
     * 在事务中执行有返回值操作。
     *
     * @param supplier 需要事务保护的写操作
     * @param <T>      返回值类型
     * @return 事务回调返回值
     * @throws NullPointerException supplier 为 {@code null} 时抛出
     * @throws RuntimeException     事务回调执行失败时抛出，并触发事务回滚
     */
    <T> T execute(Supplier<T> supplier);
}
