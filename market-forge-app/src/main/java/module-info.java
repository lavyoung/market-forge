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
    requires spring.beans;
    requires org.mybatis.spring;
    requires org.springdoc.openapi.ui;
    requires io.swagger.v3.oas.annotations;
    requires curator.framework;
    requires curator.recipes;
    requires curator.client;
    requires org.slf4j;
    requires market.forge.api;
    requires market.forge.domain;
    requires market.forge.infrastructure;
    requires market.forge.trigger;
    requires micrometer.tracing;
    requires org.apache.tomcat.embed.core;
    requires spring.data.redis;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires java.prefs;
    requires market.forge.application;
    requires dynamic.config.center.core;
    requires dynamic.config.center.zookeeper;
    requires dynamic.config.center.spring.boot.starter;
    requires dynamic.config.center.rate.limiter.spring.boot.starter;
    requires jakarta.validation;
    requires spring.security.core;
    requires spring.security.crypto;
    requires elasticsearch.java;
    requires spring.security.config;
    requires spring.security.web;

    opens com.lavyoung.marketforge.app;
    opens com.lavyoung.marketforge.app.config;
    opens com.lavyoung.marketforge.app.interceptor to spring.beans;
    opens com.lavyoung.marketforge.app.config.dcc;
    opens com.lavyoung.marketforge.app.config.security;
}
