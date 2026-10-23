package com.lavyoung.marketforge.application.award.grant;

/**
 * 奖品履约配置解码端口。
 *
 * <p>Application 层通过本端口把履约快照中的配置内容转换为类型安全的配置对象，
 * 但不感知 Jackson、Hutool 等具体序列化框架。Infrastructure 层负责提供实现。</p>
 *
 * <p>解码器必须严格校验字段类型和未知字段。配置格式错误时不得返回空值，
 * 应抛出带有统一业务错误码的异常，阻断奖品发放。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/23
 */
public interface AwardConfigDecoder {

    /**
     * 将奖品配置内容解码为指定类型。
     *
     * @param configContent 奖品履约快照中的结构化配置内容
     * @param configType    目标配置类型
     * @param <T>           目标配置对象类型
     * @return 完成字段映射和构造校验的配置对象
     * @throws NullPointerException                                       configType 为空时抛出
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 配置为空、格式错误、字段类型不匹配或配置对象校验失败时抛出
     */
    <T> T decode(String configContent, Class<T> configType);
}
