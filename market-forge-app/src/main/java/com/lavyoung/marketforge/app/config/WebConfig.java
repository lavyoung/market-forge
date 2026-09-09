package com.lavyoung.marketforge.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 统一配置。
 * <p>
 * 为触发器模块中的 HTTP 控制器集中增加版本前缀，避免各 API 契约重复维护公共路径。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/09
 */
@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

    /**
     * REST API 的统一版本前缀。
     */
    public static final String API_PATH_PREFIX = "/api/v1";

    private static final String TRIGGER_CONTROLLER_PACKAGE = "com.lavyoung.marketforge.trigger.controller";

    /**
     * 仅为触发器控制器增加统一版本前缀，不影响框架端点或其他 Spring MVC 处理器。
     *
     * @param configurer Spring MVC 路径匹配配置器
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(
                API_PATH_PREFIX,
                HandlerTypePredicate.forBasePackage(TRIGGER_CONTROLLER_PACKAGE)
        );
    }
}
