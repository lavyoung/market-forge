package com.lavyoung.marketforge.types.concurrent;

import java.util.concurrent.ExecutorService;

/**
 * 命名线程池提供者。
 * <p>
 * 应用层通过该端口按业务名称获取线程池，避免反向依赖基础设施层的线程池注册实现。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/30
 */
public interface NamedExecutorProvider {

    /**
     * 按线程池名称获取执行器。
     *
     * @param name 线程池名称
     * @return 线程池执行器
     */
    ExecutorService getExecutor(String name);
}
