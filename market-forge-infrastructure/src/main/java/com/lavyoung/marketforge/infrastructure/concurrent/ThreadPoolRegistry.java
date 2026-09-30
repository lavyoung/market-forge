package com.lavyoung.marketforge.infrastructure.concurrent;

import com.lavyoung.marketforge.types.concurrent.NamedExecutorProvider;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * 命名线程池注册表。
 * <p>
 * 作为基础设施层对应用层暴露的线程池查找实现，统一管理项目内所有业务线程池。
 * 注册表只保存已创建的执行器，不负责读取配置或构造线程池。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/29
 */
@Getter
public class ThreadPoolRegistry implements NamedExecutorProvider {

    private final Map<String, ExecutorService> executors = new LinkedHashMap<>();

    /**
     * 注册一个命名线程池。
     *
     * @param name            线程池名称
     * @param executorService 线程池执行器
     * @throws IllegalArgumentException 名称重复时抛出
     */
    public void register(String name, ExecutorService executorService) {
        if (executors.containsKey(name)) {
            throw new IllegalArgumentException("thread pool already exists: " + name);
        }
        executors.put(name, executorService);
    }

    /**
     * 按名称获取线程池执行器。
     *
     * @param name 线程池名称
     * @return 线程池执行器
     * @throws IllegalArgumentException 指定名称未注册时抛出
     */
    @Override
    public ExecutorService getExecutor(String name) {
        ExecutorService executorService = executors.get(name);
        if (executorService == null) {
            throw new IllegalArgumentException("thread pool not found: " + name);
        }
        return executorService;
    }

}
