package com.lavyoung.marketforge.app.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.redisson.codec.JsonJacksonCodec;

/**
 * Market Forge 专用 Redisson JSON 编码器。
 *
 * <p>Redisson 会根据线程上下文 ClassLoader 复制配置中的 codec，
 * 因此自定义 codec 必须提供 {@code (ClassLoader, Codec)} 复制构造器。
 * 本类在首次创建和运行时复制时都会安装相同的类型解析策略，
 * 避免编码端与解码端使用不同协议。</p>
 *
 * <p>类型策略沿用 Jackson 的非 final 类型规则，并额外支持 Java record
 * 和枚举。这样领域实体、DTO 和 VO 可以通过 Redisson 通用对象容器
 * 写入 Redis，并在读取时恢复为原始类型。</p>
 *
 * <p>允许反序列化的类型限制在 Market Forge 代码、JDK 基础类型、
 * 时间类型、集合和数组中。Redis 数据必须来自内部可信服务。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/14
 */
public final class MarketForgeJsonJacksonCodec extends JsonJacksonCodec {

    /**
     * 使用指定 ObjectMapper 创建 JSON 编码器。
     *
     * @param objectMapper 已注册项目所需模块的对象映射器
     * @throws NullPointerException ObjectMapper 为空时抛出
     */
    public MarketForgeJsonJacksonCodec(ObjectMapper objectMapper) {
        super(objectMapper);
    }

    /**
     * 根据指定 ClassLoader 复制已有编码器。
     *
     * <p>该构造器由 Redisson 的 {@code BaseCodec.copy()} 反射调用，
     * 参数类型和可见性不能随意修改。</p>
     *
     * @param classLoader Redisson 当前使用的线程上下文 ClassLoader
     * @param codec       待复制的 Market Forge JSON 编码器
     * @throws NullPointerException ClassLoader 或原编码器为空时抛出
     */
    public MarketForgeJsonJacksonCodec(ClassLoader classLoader,
                                       MarketForgeJsonJacksonCodec codec
    ) {
        super(classLoader, codec);
    }

    /**
     * 为当前 ObjectMapper 安装 Market Forge 的多态类型策略。
     *
     * <p>该方法会在首次构造和 Redisson 复制 codec 时执行，
     * 因而能够保证两个阶段使用完全相同的 JSON 类型协议。</p>
     *
     * @param objectMapper Redisson 当前 codec 持有的对象映射器
     */
    @Override
    protected void initTypeInclusion(ObjectMapper objectMapper) {
        objectMapper.setDefaultTyping(createTypeResolver());
    }

    /**
     * 创建受控的 Redis 类型解析器。
     *
     * @return 支持非 final 类型、record 和枚举的类型解析器
     */
    private ObjectMapper.DefaultTypeResolverBuilder createTypeResolver() {
        PolymorphicTypeValidator typeValidator =
                BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.lavyoung.marketforge.")
                        .allowIfSubType("java.lang.")
                        .allowIfSubType("java.time.")
                        .allowIfSubType("java.util.")
                        .allowIfSubTypeIsArray()
                        .build();

        ObjectMapper.DefaultTypeResolverBuilder typeResolverBuilder =
                new ObjectMapper.DefaultTypeResolverBuilder(
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        typeValidator
                ) {
                    /**
                     * 判断当前类型是否需要写入类型标识。
                     *
                     * @param javaType Jackson 当前处理的 Java 类型
                     * @return record、枚举或默认非 final 类型返回 {@code true}
                     */
                    @Override
                    public boolean useForType(JavaType javaType) {
                        return javaType.isRecordType()
                                || javaType.isEnumType()
                                || super.useForType(javaType);
                    }
                };

        typeResolverBuilder.init(JsonTypeInfo.Id.CLASS, null);
        typeResolverBuilder.inclusion(JsonTypeInfo.As.PROPERTY);
        return typeResolverBuilder;
    }
}
