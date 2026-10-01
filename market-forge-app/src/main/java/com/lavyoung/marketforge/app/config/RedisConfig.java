package com.lavyoung.marketforge.app.config;


import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis 客户端配置。
 * <p>
 * 根据应用的 Redis 外部化配置创建并托管 {@link RedissonClient}。客户端统一使用 JSON 编码，
 * 便于活动、策略等领域对象在 Redis 中以可读结构缓存，也避免默认二进制序列化带来的类型兼容问题。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RedisConfigProperties.class)
public class RedisConfig {

    /**
     * 创建用于访问 Redis 的 Redisson 客户端。
     *
     * @param applicationContext Spring 应用上下文
     * @param properties         Redis 连接与连接池配置
     * @return 配置完成的 Redisson 客户端
     * @throws IllegalArgumentException Redis 配置无法被 Redisson 接受时抛出
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(ConfigurableApplicationContext applicationContext, RedisConfigProperties properties) {
        Config config = new Config();
        // 统一使用 JSON 编码，保证跨模块缓存对象具备稳定、可读的序列化格式。
        config.setCodec(new JsonJacksonCodec());
        config.useSingleServer()
                .setAddress("redis://" + properties.getHost() + ":" + properties.getPort())
                .setPassword(properties.getPassword())
                .setConnectionPoolSize(properties.getPool().getPoolSize())
                .setConnectionMinimumIdleSize(properties.getPool().getMinIdleSize())
                .setIdleConnectionTimeout(properties.getPool().getIdleTimeout())
                .setConnectTimeout(properties.getPool().getConnectTimeout())
                .setRetryAttempts(properties.getPool().getRetryAttempts())
                .setRetryInterval(properties.getPool().getRetryInterval())
                .setPingConnectionInterval(properties.getPool().getPingInterval())
                .setKeepAlive(properties.getPool().isKeepAlive());
        return Redisson.create(config);
    }
}
