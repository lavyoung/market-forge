/**
 * 通用
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @date 2026/09/01
 */module market.forge.types {
    exports com.lavyoung.marketforge.types.common;
    exports com.lavyoung.marketforge.types.domain;
    exports com.lavyoung.marketforge.types.exception;
    exports com.lavyoung.marketforge.types.model;
    exports com.lavyoung.marketforge.types.domain.strategy;
    exports com.lavyoung.marketforge.types.messaging;
    exports com.lavyoung.marketforge.types.utils;
    requires static lombok;
    requires org.slf4j;
    requires spring.context;
}