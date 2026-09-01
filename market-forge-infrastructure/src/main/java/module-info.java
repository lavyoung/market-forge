/**
 * 基础设施
 *
 * @author lavyoung
 * @email lavyoung1325@outlook.com
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
    requires spring.context;
}