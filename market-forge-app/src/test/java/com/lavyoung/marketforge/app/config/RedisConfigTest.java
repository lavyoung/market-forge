package com.lavyoung.marketforge.app.config;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import org.junit.jupiter.api.Test;
import org.redisson.client.codec.BaseCodec;
import org.redisson.client.handler.State;
import org.redisson.codec.JsonJacksonCodec;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 验证 Redisson JSON 编码器的领域对象序列化契约。
 *
 * <p>测试不连接真实 Redis，只验证配置创建的 codec 能够完整编码并恢复
 * 包含 Java 时间字段的活动领域实体。</p>
 */
class RedisConfigTest {

    private static final LocalDateTime BEGIN_TIME = LocalDateTime.of(2026, 10, 1, 0, 0);
    private static final LocalDateTime END_TIME = LocalDateTime.of(2026, 12, 31, 23, 59, 59);

    /**
     * Given 包含 LocalDateTime 的活动实体，When 使用项目 Redisson codec 往返转换，
     * Then JSON 保留类型和 ISO 时间信息并恢复原始领域类型。
     *
     * @throws Exception codec 创建反射或 JSON 编解码失败时抛出
     */
    @Test
    void shouldRoundTripActivityEntityWithJavaTimeAndTypeInformation() throws Exception {
        // Given
        JsonJacksonCodec codec = createJsonCodec();

        // When & Then
        assertRoundTrip(codec, activityCacheSample());
    }

    /**
     * Given 生产配置创建的编码器，When Redisson 按运行时机制复制编码器，
     * Then 复制结果仍保留自定义类型策略，并能恢复 record 与 Java 时间字段。
     *
     * @throws Exception codec 创建、复制或 JSON 编解码失败时抛出
     */
    @Test
    void shouldRetainRecordAndJavaTimeSupportAfterRedissonCopiesCodec() throws Exception {
        // Given
        JsonJacksonCodec original = createJsonCodec();

        // When
        JsonJacksonCodec copied = BaseCodec.copy(getClass().getClassLoader(), original);

        // Then
        assertInstanceOf(MarketForgeJsonJacksonCodec.class, copied);
        assertRoundTrip(copied, activityCacheSample());
    }

    /**
     * 验证指定编码器可以完整写入并恢复活动缓存样本。
     *
     * @param codec  待验证的 Redisson JSON 编码器
     * @param source 原始活动缓存样本
     * @throws Exception JSON 编解码失败时抛出
     */
    private void assertRoundTrip(JsonJacksonCodec codec, ActivityCacheSample source) throws Exception {
        ByteBuf encoded = codec.getValueEncoder().encode(source);
        ByteBuf decodeBuffer = encoded.copy();

        try {
            // When
            String json = encoded.toString(StandardCharsets.UTF_8);
            Object decodedValue = codec.getValueDecoder().decode(decodeBuffer, new State());
            ActivityCacheSample decoded = assertInstanceOf(ActivityCacheSample.class, decodedValue);

            // Then
            assertAll(
                    () -> assertTrue(json.contains("@class")),
                    () -> assertTrue(json.contains("2026-10-01T00:00:00")),
                    () -> assertEquals(source, decoded)
            );
        } finally {
            ReferenceCountUtil.release(decodeBuffer);
            ReferenceCountUtil.release(encoded);
        }
    }

    /**
     * 调用生产配置中的 codec 工厂，确保测试覆盖真实配置而非重复构造测试对象。
     *
     * @return 生产配置创建的 Redisson JSON 编码器
     * @throws ReflectiveOperationException 无法访问 codec 工厂时抛出
     */
    private JsonJacksonCodec createJsonCodec() throws ReflectiveOperationException {
        Method factoryMethod = RedisConfig.class.getDeclaredMethod("createJsonCodec");
        factoryMethod.setAccessible(true);
        return (JsonJacksonCodec) factoryMethod.invoke(new RedisConfig());
    }

    /**
     * 创建包含 Java 时间字段的活动领域实体。
     *
     * @return 测试活动实体
     */
    private ActivityCacheSample activityCacheSample() {
        return new ActivityCacheSample(
                100_301L,
                "九宫格活动抽奖",
                "Redisson JSON 编码测试",
                BEGIN_TIME,
                END_TIME,
                100_000,
                99_999,
                100_301L,
                900_901_001L,
                new BigDecimal("0.0100"),
                "open"
        );
    }

    /**
     * 模拟活动缓存中同时包含标量与 Java 时间字段的数据结构。
     *
     * @param activityId        活动标识
     * @param activityName      活动名称
     * @param activityDesc      活动说明
     * @param beginDateTime     活动开始时间
     * @param endDateTime       活动结束时间
     * @param stockCount        活动库存总量
     * @param stockCountSurplus 活动剩余库存
     * @param activityCountId   活动次数配置标识
     * @param strategyId        抽奖策略标识
     * @param awardRate         奖品概率，用于验证 java.math 类型白名单
     * @param state             活动状态
     */
    private record ActivityCacheSample(
            Long activityId,
            String activityName,
            String activityDesc,
            LocalDateTime beginDateTime,
            LocalDateTime endDateTime,
            Integer stockCount,
            Integer stockCountSurplus,
            Long activityCountId,
            Long strategyId,
            BigDecimal awardRate,
            String state
    ) {
    }
}
