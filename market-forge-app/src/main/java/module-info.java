/**
 * 应用启动与模块装配层。
 * <p>
 * 负责启动 Spring Boot，并装配入站适配器和基础设施实现；业务用例位于
 * {@code market.forge.application} 模块。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @date 2026/09/01
 */module market.forge.app {
    requires static lombok;
    requires redisson;
    requires spring.boot;
    requires spring.boot.autoconfigure;
    requires spring.context;
    requires spring.web;
    requires spring.webmvc;
    requires org.mybatis.spring;
    requires org.springdoc.openapi.ui;
    requires io.swagger.v3.oas.annotations;

    requires market.forge.api;
    requires market.forge.infrastructure;
    requires market.forge.trigger;
    requires micrometer.tracing;
    requires org.apache.tomcat.embed.core;

    opens com.lavyoung.marketforge.app;
    opens com.lavyoung.marketforge.app.config;
}
