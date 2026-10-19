/**
 * 触发器
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @date 2026/09/01
 */
module market.forge.trigger {
    requires market.forge.api;
    requires market.forge.application;
    requires spring.web;
    requires spring.beans;
    requires market.forge.types;
    requires spring.context;
    requires jakarta.validation;
    requires org.mapstruct;
    requires org.slf4j;
    requires static lombok;
    requires dynamic.config.center.rate.limiter;
    requires spring.cloud.commons;
    requires spring.webmvc;
    requires org.apache.tomcat.embed.core;
    requires spring.security.core;
    requires spring.security.web;

    exports com.lavyoung.marketforge.trigger.security;

    opens com.lavyoung.marketforge.trigger.assembler;
    opens com.lavyoung.marketforge.trigger.circuitbreaker;
    opens com.lavyoung.marketforge.trigger.controller;
    opens com.lavyoung.marketforge.trigger.exception;
    opens com.lavyoung.marketforge.trigger.job;
}
