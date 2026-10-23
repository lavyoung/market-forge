package com.lavyoung.marketforge.infrastructure.award.grant;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lavyoung.marketforge.application.award.grant.AwardConfigDecoder;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 基于 Jackson 的奖品履约配置解码适配器。
 *
 * <p>本适配器实现 Application 层定义的配置解码端口，负责将数据库或可靠消息中的
 * JSON 配置转换为类型安全的配置对象。它只处理序列化技术，不决定奖品发放规则。</p>
 *
 * <p>解码过程强制拒绝未知字段，避免运营配置拼写错误后被 Jackson 静默忽略。
 * 本类使用不可变的 {@code ObjectReader} 配置，不修改 Spring Boot 管理的全局
 * {@link ObjectMapper}。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/23
 */
@Component
@RequiredArgsConstructor
public class JacksonAwardConfigDecoder implements AwardConfigDecoder {

    private final ObjectMapper objectMapper;

    @Override
    public <T> T decode(String configContent, Class<T> configType) {
        Objects.requireNonNull(configType, "configType must not be null");
        if (configContent == null || configContent.isBlank()) {
            throw BusinessException.of(BusinessResponseCode.AWARD_CONFIG_INVALID, configType.getSimpleName());
        }
        try {
            T config = objectMapper.readerFor(configType)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(configContent);
            if (config == null) {
                throw BusinessException.of(BusinessResponseCode.AWARD_CONFIG_INVALID, configType.getSimpleName());
            }
            return config;
        } catch (JsonProcessingException exception) {
            throw BusinessException.of(
                    BusinessResponseCode.AWARD_CONFIG_INVALID,
                    exception,
                    configType.getSimpleName()
            );
        }
    }
}
