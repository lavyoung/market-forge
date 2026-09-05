/**
 * 抽奖策略领域模块，承载领域模型、仓储端口与核心领域服务。
 */
module market.forge.domain {
    exports com.lavyoung.marketforge.domain.strategy.repository;
    exports com.lavyoung.marketforge.domain.strategy.model.entity;
    requires spring.context;
    requires org.slf4j;
    requires static lombok;
    requires spring.core;
    requires org.apache.commons.lang3;
    requires market.forge.types;
    requires jakarta.annotation;
}
