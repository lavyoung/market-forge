package com.lavyoung.marketforge.infrastructure.concurrent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * 线程池基础设施配置。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/29
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(ThreadPoolProperties.class)
public class ThreadPoolConfiguration implements DisposableBean {
    private final ThreadPoolProperties properties;

    private ThreadPoolRegistry registry;

    public ThreadPoolConfiguration(ThreadPoolProperties properties) {
        this.properties = properties;
    }

    @Bean
    public ThreadPoolRegistry threadPoolRegistry() {
        ThreadPoolRegistry threadPoolRegistry = new ThreadPoolRegistry();
        if (properties.isEnabled()) {
            properties.getPools().forEach((name, pool) -> threadPoolRegistry.register(name, ThreadPoolFactory.create(name, pool)));
        }
        this.registry = threadPoolRegistry;
        return threadPoolRegistry;
    }


    @Override
    public void destroy() throws Exception {
        if (registry == null) {
            return;
        }
        for (Map.Entry<String, ExecutorService> entry : registry.getExecutors().entrySet()) {
            shutdown(entry.getKey(), entry.getValue());
        }
    }

    private void shutdown(String name, ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            log.info("线程池已关闭 name={}", name);
        } catch (InterruptedException exception) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
            log.warn("线程池关闭被中断 name={}", name, exception);
        }
    }
}
