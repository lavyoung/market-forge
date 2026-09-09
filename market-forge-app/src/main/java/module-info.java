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
    requires market.forge.api;
}
