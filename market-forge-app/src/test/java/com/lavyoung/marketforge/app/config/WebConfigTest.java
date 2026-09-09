package com.lavyoung.marketforge.app.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;

import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 验证 REST API 版本前缀仅应用于触发器控制器。
 */
class WebConfigTest {

    /**
     * Given Web MVC 路径配置，When 注册统一前缀，Then 不匹配 trigger 包之外的配置类。
     */
    @Test
    void shouldPrefixOnlyTriggerControllers() {
        // Given
        TestPathMatchConfigurer configurer = new TestPathMatchConfigurer();

        // When
        new WebConfig().configurePathMatch(configurer);

        // Then
        Map<String, Predicate<Class<?>>> prefixes = configurer.pathPrefixes();
        assertEquals(1, prefixes.size());
        Predicate<Class<?>> triggerControllerPredicate = prefixes.get(WebConfig.API_PATH_PREFIX);
        assertFalse(triggerControllerPredicate.test(WebConfig.class));
    }

    /**
     * 暴露 Spring 配置器的受保护读取方法，供单元测试验证已注册的路径前缀。
     */
    private static final class TestPathMatchConfigurer extends PathMatchConfigurer {

        private Map<String, Predicate<Class<?>>> pathPrefixes() {
            return getPathPrefixes();
        }
    }
}
