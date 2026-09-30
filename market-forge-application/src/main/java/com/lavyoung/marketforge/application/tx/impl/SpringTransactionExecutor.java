package com.lavyoung.marketforge.application.tx.impl;

import com.lavyoung.marketforge.application.tx.ITransactionExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;
import java.util.function.Supplier;


/**
 * 基于 Spring {@link TransactionTemplate} 的应用层事务执行器。
 * <p>
 * 通过编程式事务为 application 层提供显式事务边界，避免同类内部调用导致
 * {@code @Transactional} 代理不生效，也让跨聚合写入流程的事务范围更加可见。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
public class SpringTransactionExecutor implements ITransactionExecutor {

    private final TransactionTemplate transactionTemplate;

    @Override
    public void execute(Runnable action) {
        Objects.requireNonNull(action, "action must not be null");
        transactionTemplate.executeWithoutResult(status -> action.run());
    }

    @Override
    public <T> T execute(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier must not be null");
        return transactionTemplate.execute(status -> supplier.get());
    }
}
