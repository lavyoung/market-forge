package com.lavyoung.marketforge.application.award.grant;

import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link InternalCreditAwardConfigValidator} 单元测试。
 *
 * <p>验证积分配置校验器声明正确的协议标识，并复用统一配置解码端口执行结构和
 * 业务取值校验。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/23
 */
@ExtendWith(MockitoExtension.class)
class InternalCreditAwardConfigValidatorTest {

    private static final String CONFIG_CONTENT = "{\"amount\":100}";

    @Mock
    private AwardConfigDecoder configDecoder;

    /**
     * 验证校验器声明内部积分版本 1 配置协议。
     */
    @Test
    void shouldDeclareInternalCreditVersionOneProtocol() {
        // Given
        InternalCreditAwardConfigValidator validator = validator();

        // When / Then
        assertEquals(InternalCreditAwardConfig.AWARD_TYPE, validator.awardType());
        assertEquals(InternalCreditAwardConfig.CONFIG_VERSION, validator.configVersion());
    }

    /**
     * 验证合法配置通过统一解码端口完成校验。
     */
    @Test
    void shouldValidateConfigThroughDecoder() {
        // Given
        InternalCreditAwardConfigValidator validator = validator();
        InternalCreditAwardConfig expected = new InternalCreditAwardConfig(100L);
        when(configDecoder.decode(CONFIG_CONTENT, InternalCreditAwardConfig.class)).thenReturn(expected);

        // When
        validator.validate(CONFIG_CONTENT);

        // Then
        verify(configDecoder).decode(CONFIG_CONTENT, InternalCreditAwardConfig.class);
    }

    /**
     * 验证解码器判定配置非法时原业务异常继续向上抛出。
     */
    @Test
    void shouldPropagateInvalidConfigException() {
        // Given
        InternalCreditAwardConfigValidator validator = validator();
        BusinessException decodeFailure = BusinessException.of(BusinessResponseCode.AWARD_CONFIG_INVALID, "internal_credit", 1);
        when(configDecoder.decode(CONFIG_CONTENT, InternalCreditAwardConfig.class)).thenThrow(decodeFailure);

        // When
        BusinessException exception = assertThrows(BusinessException.class, () -> validator.validate(CONFIG_CONTENT));

        // Then
        assertSame(decodeFailure, exception);
    }

    private InternalCreditAwardConfigValidator validator() {
        return new InternalCreditAwardConfigValidator(configDecoder);
    }
}
