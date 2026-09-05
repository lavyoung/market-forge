/**
 * Market Forge 基础设施模块。
 * <p>
 * 提供数据库访问、对象映射、Redis 缓存及领域仓储端口实现。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @date 2026/09/01
 */
module market.forge.infrastructure {
    requires java.compiler;
    requires static lombok;
    requires market.forge.domain;
    requires market.forge.types;
    requires org.mapstruct;
    requires org.mybatis;
    requires redisson;
    requires org.slf4j;
    requires spring.beans;
    requires spring.context;
    requires mybatis.plus.core;
    requires mybatis.plus.annotation;
}
