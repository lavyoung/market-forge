package com.lavyoung.marketforge.infrastructure.award.grant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lavyoung.marketforge.application.award.grant.InternalCreditAwardConfig;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link JacksonAwardConfigDecoder} 单元测试。
 *
 * <p>验证奖品配置只能在 JSON 结构、字段名称、字段类型和配置对象自身约束全部满足时
 * 完成解码，所有不合法内容统一转换为奖品配置业务异常。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/23
 */
class JacksonAwardConfigDecoderTest {

    private final JacksonAwardConfigDecoder decoder = new JacksonAwardConfigDecoder(new ObjectMapper());

    /**
     * 验证合法的积分配置 JSON 能够转换为类型安全的配置对象。
     */
    @Test
    void shouldDecodeValidInternalCreditConfig() {
        // Given
        String configContent = "{\"amount\":100}";

        // When
        InternalCreditAwardConfig config = decoder.decode(configContent, InternalCreditAwardConfig.class);

        // Then
        assertEquals(100L, config.amount());
    }

    /**
     * 验证未知字段会被严格拒绝，防止运营配置字段拼写错误后被静默忽略。
     */
    @Test
    void shouldRejectUnknownProperty() {
        assertInvalidConfig("{\"amount\":100,\"unexpected\":true}");
    }

    /**
     * 验证字段类型不匹配和非法 JSON 都会转换为统一业务异常。
     */
    @Test
    void shouldRejectInvalidJsonStructure() {
        assertInvalidConfig("{\"amount\":\"not-a-number\"}");
        assertInvalidConfig("{\"amount\":100");
    }

    /**
     * 验证配置对象构造约束失败时不会泄漏 Jackson 底层异常。
     */
    @Test
    void shouldRejectInvalidConfigValue() {
        assertInvalidConfig("{\"amount\":0}");
        assertInvalidConfig("{\"amount\":-1}");
        assertInvalidConfig("{}");
    }

    /**
     * 验证空内容和 JSON null 不能生成有效配置对象。
     */
    @Test
    void shouldRejectEmptyOrNullJsonContent() {
        assertInvalidConfig(null);
        assertInvalidConfig(" ");
        assertInvalidConfig("null");
    }

    /**
     * 验证目标配置类型是调用契约的必要参数。
     */
    @Test
    void shouldRejectNullConfigType() {
        // When
        NullPointerException exception = assertThrows(
                NullPointerException.class, () -> decoder.decode("{\"amount\":100}", null));

        // Then
        assertEquals("configType must not be null", exception.getMessage());
    }

    /**
     * 断言指定配置内容会被统一判定为奖品配置非法。
     *
     * @param configContent 待解码的配置内容
     */
    private void assertInvalidConfig(String configContent) {
        // When
        BusinessException exception = assertThrows(BusinessException.class,
                () -> decoder.decode(configContent, InternalCreditAwardConfig.class));

        // Then
        assertEquals(BusinessResponseCode.AWARD_CONFIG_INVALID, exception.getResponseCode());
    }
}
