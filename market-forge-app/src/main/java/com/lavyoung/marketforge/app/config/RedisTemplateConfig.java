package com.lavyoung.marketforge.app.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Spring Data Redis 模板配置。
 * <p>
 * 本配置提供项目默认的 {@link RedisTemplate} 实例，统一约定 Redis Key 使用字符串序列化，
 * Value 和 Hash Value 使用 Jackson JSON 序列化。该模板主要面向需要直接使用
 * Spring Data Redis API 的组件；Redisson 客户端仍由 {@link RedisConfig} 单独配置。
 * <p>
 * JSON 序列化器会写入类型信息，适合服务内部可信缓存对象的读写；不要将该模板用于反序列化
 * 外部不可信 Redis 数据。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/01
 */
@Configuration(proxyBeanMethods = false)
public class RedisTemplateConfig {

    /**
     * 创建全局默认的 RedisTemplate。
     * <p>
     * Key 与 Hash Key 使用字符串序列化，保证 Redis 中的键名可读；Value 与 Hash Value 使用
     * 支持 Java 时间类型的 JSON 序列化器，避免 {@code LocalDateTime} 等类型被写成时间戳或无法反序列化。
     *
     * @param connectionFactory Spring Data Redis 连接工厂
     * @return 已初始化完成的 RedisTemplate 实例
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = createJsonRedisSerializer();
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
        redisTemplate.setValueSerializer(jsonRedisSerializer);
        redisTemplate.setHashValueSerializer(jsonRedisSerializer);

        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * 创建支持类型信息和 Java 时间类型的 JSON Redis 序列化器。
     * <p>
     * {@link JavaTimeModule} 负责处理 {@code LocalDate}、{@code LocalDateTime}、{@code Instant}
     * 等类型；关闭时间戳输出后，缓存中的时间值会以更易读的 ISO-8601 字符串形式保存。
     * 默认类型信息用于让 RedisTemplate 在读取 {@link Object} 类型值时恢复原始对象类型。
     *
     * @return GenericJackson2JsonRedisSerializer JSON 序列化器
     */
    private GenericJackson2JsonRedisSerializer createJsonRedisSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL_AND_ENUMS,
                JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
