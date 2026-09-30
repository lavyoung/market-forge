package com.lavyoung.marketforge.infrastructure.concurrent;

import com.lavyoung.marketforge.types.concurrent.RejectedPolicy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 线程池配置属性
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/29
 */
@ConfigurationProperties(prefix = "market-forge.thread-pool")
@Data
public class ThreadPoolProperties {

    private boolean enabled = true;

    private Map<String, Pool> pools = new LinkedHashMap<>();

    @Data
    public static class Pool {

        private int corePoolSize = 2;

        private int maximumPoolSize = 4;

        private int queueCapacity = 1000;

        private long keepAliveSeconds = 60;

        private int awaitTerminationSeconds = 30;

        private RejectedPolicy rejectedPolicy = RejectedPolicy.CALLER_RUNS;

    }
}
