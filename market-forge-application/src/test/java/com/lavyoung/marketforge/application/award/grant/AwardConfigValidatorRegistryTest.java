package com.lavyoung.marketforge.application.award.grant;

import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AwardConfigValidatorRegistry} 单元测试。
 *
 * <p>验证配置校验器能够按照奖品类型和配置版本稳定路由，并在协议缺失、声明非法
 * 或重复注册时尽早失败。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/23
 */
@ExtendWith(MockitoExtension.class)
class AwardConfigValidatorRegistryTest {

    private static final String AWARD_TYPE = "internal_credit";
    private static final int CONFIG_VERSION = 1;
    private static final String CONFIG_CONTENT = "{\"amount\":100}";

    @Mock
    private AwardConfigValidator validator;

    /**
     * 验证注册表会按类型和版本调用唯一匹配的校验器。
     */
    @Test
    void shouldRouteConfigToMatchingValidator() {
        // Given
        when(validator.awardType()).thenReturn(AWARD_TYPE);
        when(validator.configVersion()).thenReturn(CONFIG_VERSION);
        AwardConfigValidatorRegistry registry = new AwardConfigValidatorRegistry(List.of(validator));

        // When
        registry.validate(AWARD_TYPE, CONFIG_VERSION, CONFIG_CONTENT);

        // Then
        verify(validator).validate(CONFIG_CONTENT);
        assertTrue(registry.supports(AWARD_TYPE, CONFIG_VERSION));
        assertFalse(registry.supports(AWARD_TYPE, 2));
    }

    /**
     * 验证没有对应校验器的配置协议会返回统一奖品配置错误。
     */
    @Test
    void shouldRejectUnsupportedConfigProtocol() {
        // Given
        AwardConfigValidatorRegistry registry = new AwardConfigValidatorRegistry(List.of());

        // When
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> registry.validate(AWARD_TYPE, CONFIG_VERSION, CONFIG_CONTENT));

        // Then
        assertEquals(BusinessResponseCode.AWARD_CONFIG_INVALID, exception.getResponseCode());
    }

    /**
     * 验证业务调用传入空类型或非法版本时不会尝试查找校验器。
     */
    @Test
    void shouldRejectInvalidBusinessKey() {
        // Given
        AwardConfigValidatorRegistry registry = new AwardConfigValidatorRegistry(List.of());

        // When
        BusinessException nullTypeException = assertThrows(
                BusinessException.class, () -> registry.validate(null, CONFIG_VERSION, CONFIG_CONTENT));
        BusinessException blankTypeException = assertThrows(
                BusinessException.class, () -> registry.validate(" ", CONFIG_VERSION, CONFIG_CONTENT));
        BusinessException invalidVersionException = assertThrows(
                BusinessException.class, () -> registry.validate(AWARD_TYPE, 0, CONFIG_CONTENT));

        // Then
        assertEquals(BusinessResponseCode.AWARD_CONFIG_INVALID, nullTypeException.getResponseCode());
        assertEquals(BusinessResponseCode.AWARD_CONFIG_INVALID, blankTypeException.getResponseCode());
        assertEquals(BusinessResponseCode.AWARD_CONFIG_INVALID, invalidVersionException.getResponseCode());
        assertFalse(registry.supports(null, CONFIG_VERSION));
        assertFalse(registry.supports(" ", CONFIG_VERSION));
        assertFalse(registry.supports(AWARD_TYPE, 0));
    }

    /**
     * 验证相同类型和版本不能注册多个校验器。
     */
    @Test
    void shouldRejectDuplicateValidatorProtocol() {
        // Given
        AwardConfigValidator first = validator(AWARD_TYPE, CONFIG_VERSION);
        AwardConfigValidator second = validator(AWARD_TYPE, CONFIG_VERSION);

        // When
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new AwardConfigValidatorRegistry(List.of(first, second)));

        // Then
        assertEquals("duplicate award config validator: internal_credit:1", exception.getMessage());
    }

    /**
     * 验证校验器声明空类型或非法版本时阻止注册表创建。
     */
    @Test
    void shouldRejectInvalidValidatorDeclaration() {
        // Given
        AwardConfigValidator blankTypeValidator = validator(" ", CONFIG_VERSION);
        AwardConfigValidator invalidVersionValidator = validator(AWARD_TYPE, 0);

        // When
        IllegalStateException blankTypeException = assertThrows(
                IllegalStateException.class,
                () -> new AwardConfigValidatorRegistry(List.of(blankTypeValidator)));
        IllegalStateException invalidVersionException = assertThrows(
                IllegalStateException.class,
                () -> new AwardConfigValidatorRegistry(List.of(invalidVersionValidator)));

        // Then
        assertEquals("award config validator type must not be blank", blankTypeException.getMessage());
        assertEquals("award config validator version must be positive", invalidVersionException.getMessage());
    }

    /**
     * 验证空校验器集合和空元素都会在启动装配阶段被拒绝。
     */
    @Test
    void shouldRejectNullValidatorCollectionOrElement() {
        // Given
        List<AwardConfigValidator> validators = new ArrayList<>();
        validators.add(null);

        // When
        NullPointerException nullListException = assertThrows(
                NullPointerException.class, () -> new AwardConfigValidatorRegistry(null));
        NullPointerException nullElementException = assertThrows(
                NullPointerException.class, () -> new AwardConfigValidatorRegistry(validators));

        // Then
        assertEquals("validatorList must not be null", nullListException.getMessage());
        assertEquals("award config validator must not be null", nullElementException.getMessage());
    }

    private AwardConfigValidator validator(String awardType, int configVersion) {
        return new AwardConfigValidator() {
            @Override
            public String awardType() {
                return awardType;
            }

            @Override
            public int configVersion() {
                return configVersion;
            }

            @Override
            public void validate(String configContent) {
                // 注册表装配测试不执行具体配置校验。
            }
        };
    }
}
