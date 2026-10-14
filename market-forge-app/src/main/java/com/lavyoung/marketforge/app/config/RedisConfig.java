package com.lavyoung.marketforge.app.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redis 客户端配置。
 *
 * <p>根据应用的 Redis 外部化配置创建并托管 {@link RedissonClient}。
 * Redisson 统一使用带类型信息的 JSON 编码，并显式支持 Java 时间类型，
 * 保证活动、策略等领域对象可以稳定写入 Redis 并恢复为原始类型。</p>
 *
 * <p>Redis 中的数据属于服务内部可信缓存，不允许使用该编码器处理外部传入的
 * 不可信类型信息。</p>
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
     * <p>连接参数由 {@link RedisConfigProperties} 提供，值编码统一使用
     * 支持 Java 时间类型的 {@link JsonJacksonCodec}。</p>
     *
     * @param properties Redis 连接与连接池配置
     * @return 配置完成并已启动的 Redisson 客户端
     * @throws IllegalArgumentException Redis 地址、连接池或超时参数不合法时抛出
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(RedisConfigProperties properties) {
        Config config = new Config();
        // 统一使用 JSON 编码，保证跨模块缓存对象具备稳定、可读的序列化格式。
        config.setCodec(createJsonCodec());
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

    /**
     * 创建支持 Java 时间类型和 record 类型恢复的 Redisson JSON 编码器。
     *
     * <p>{@link JavaTimeModule} 负责处理 {@code LocalDate}、
     * {@code LocalDateTime} 和 {@code Instant} 等时间类型。
     * {@link MarketForgeJsonJacksonCodec} 保证 Redisson 复制 codec 后
     * 仍然使用相同的受控类型协议。</p>
     *
     * @return Market Forge 专用 Redisson JSON 编码器
     */
    private JsonJacksonCodec createJsonCodec() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new MarketForgeJsonJacksonCodec(objectMapper);
    }
}
