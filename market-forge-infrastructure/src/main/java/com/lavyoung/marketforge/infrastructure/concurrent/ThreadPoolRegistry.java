package com.lavyoung.marketforge.infrastructure.concurrent;

import com.lavyoung.marketforge.types.concurrent.NamedExecutorProvider;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 *
 * 线程池注册表。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/29
 */
@Getter
public class ThreadPoolRegistry implements NamedExecutorProvider {

    private final Map<String, ExecutorService> executors = new LinkedHashMap<>();

    public void register(String name, ExecutorService executorService) {
        if (executors.containsKey(name)) {
            throw new IllegalArgumentException("thread pool already exists: " + name);
        }
        executors.put(name, executorService);
    }

    @Override
    public ExecutorService getExecutor(String name) {
        ExecutorService executorService = executors.get(name);
        if (executorService == null) {
            throw new IllegalArgumentException("thread pool not found: " + name);
        }
        return executorService;
    }

}
