/**
 * 抽奖策略领域模块，承载领域模型、仓储端口与核心领域服务。
 */
module market.forge.domain {
    exports com.lavyoung.marketforge.domain.activity.model.entity;
    exports com.lavyoung.marketforge.domain.strategy.repository;
    exports com.lavyoung.marketforge.domain.strategy.model.entity;
    exports com.lavyoung.marketforge.domain.strategy.model.vo;
    exports com.lavyoung.marketforge.domain.strategy.service;
    exports com.lavyoung.marketforge.domain.strategy.service.armorcy;
    exports com.lavyoung.marketforge.domain.activity.repository;
    exports com.lavyoung.marketforge.domain.activity.model.aggregate;
    exports com.lavyoung.marketforge.domain.activity.service.rule.factory;
    exports com.lavyoung.marketforge.domain.activity.service.impl;
    exports com.lavyoung.marketforge.domain.activity.service;
    exports com.lavyoung.marketforge.domain.activity.event;
    exports com.lavyoung.marketforge.domain.strategy.event;
    exports com.lavyoung.marketforge.domain.activity.model.vo;

    requires spring.context;
    requires org.slf4j;
    requires static lombok;
    requires spring.core;
    requires org.apache.commons.lang3;
    requires market.forge.types;
    requires jakarta.annotation;


    opens com.lavyoung.marketforge.domain.strategy.service.impl;
    opens com.lavyoung.marketforge.domain.strategy.service.armorcy.impl;
    opens com.lavyoung.marketforge.domain.strategy.service.rule.filter.factory;
    opens com.lavyoung.marketforge.domain.strategy.service.rule.filter.impl;
    opens com.lavyoung.marketforge.domain.strategy.service.rule.chain.factory;
    opens com.lavyoung.marketforge.domain.strategy.service.rule.chain.impl;
    opens com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory;
    opens com.lavyoung.marketforge.domain.strategy.service.rule.tree.impl;
    opens com.lavyoung.marketforge.domain.activity.service.armory;
    opens com.lavyoung.marketforge.domain.activity.service.armory.impl;
    opens com.lavyoung.marketforge.domain.activity.service.rule;
    opens com.lavyoung.marketforge.domain.activity.service.rule.impl;
}
