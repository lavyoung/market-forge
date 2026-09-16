package com.lavyoung.marketforge.app.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

/**
 * Market Forge OpenAPI 文档配置。
 * <p>
 * 统一声明接口文档的名称、版本、维护者以及业务分组。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/16
 */
@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
        info = @Info(
                title = "Market Forge API",
                version = "v1",
                description = "Market Forge 营销抽奖平台 REST API",
                contact = @Contact(
                        name = "lavyoung",
                        email = "lavyoung1325@outlook.com"
                ),
                license = @License(
                        name = "Project License"
                )
        ),
        tags = {
                @Tag(
                        name = "策略抽奖",
                        description = "策略装配、奖品查询及抽奖接口"
                )
        }
)
public class OpenApiConfig {
}
