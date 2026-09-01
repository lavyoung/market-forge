package com.lavyoung.marketforge.app.config;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code spring.data.redis} 命名空间下的 Redis 配置属性。
 *
 * @author lavyoung
 * @version 1.0.0
 */
@ConfigurationProperties(value = "spring.data.redis")
@Getter
@Setter
public class RedisConfigProperties {

    private String host = "localhost";
    private int port = 6379;
    private String password;
    private String prefix;
    private Pool pool = new Pool();


    /**
     * Redisson 单节点连接池及连接保活配置。
     */
    @Getter
    @Setter
    @ToString
    public static class Pool {

        private int poolSize = 5;
        private int minIdleSize = 1;
        private int idleTimeout = 3000;
        private int connectTimeout = 3000;
        private int retryAttempts = 1;
        private int retryInterval = 2000;
        private int pingInterval = 5000;
        private boolean keepAlive = true;
    }
}
