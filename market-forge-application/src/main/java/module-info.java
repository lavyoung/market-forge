/**
 * Market Forge 应用层模块。
 * <p>
 * 负责用例编排、事务边界与领域模型转换，不包含 HTTP、RPC、消息队列等传输协议细节。
 */
module market.forge.application {
    requires market.forge.domain;
    requires market.forge.types;
    requires org.slf4j;
    requires spring.context;
    requires spring.tx;
    requires static lombok;
    requires org.apache.commons.lang3;

    exports com.lavyoung.marketforge.application.strategy.model;
    exports com.lavyoung.marketforge.application.strategy.service;
    exports com.lavyoung.marketforge.application.strategy.service.impl;
    exports com.lavyoung.marketforge.application.strategy.messaging;
    exports com.lavyoung.marketforge.application.activity.model;
    exports com.lavyoung.marketforge.application.activity.service;
    exports com.lavyoung.marketforge.application.activity.service.impl;
    exports com.lavyoung.marketforge.application.activity.messaging;
    exports com.lavyoung.marketforge.application.message;
    exports com.lavyoung.marketforge.application.award.model;
    exports com.lavyoung.marketforge.application.award.service;
    exports com.lavyoung.marketforge.application.award.service.impl;
    exports com.lavyoung.marketforge.application.award.messaging;

    opens com.lavyoung.marketforge.application.strategy.service;
    opens com.lavyoung.marketforge.application.strategy.service.impl;
    opens com.lavyoung.marketforge.application.strategy.messaging;
    opens com.lavyoung.marketforge.application.activity.model;
    opens com.lavyoung.marketforge.application.activity.service;
    opens com.lavyoung.marketforge.application.activity.service.impl;
    opens com.lavyoung.marketforge.application.activity.messaging;
    opens com.lavyoung.marketforge.application.message;
    opens com.lavyoung.marketforge.application.award.service;
    opens com.lavyoung.marketforge.application.award.service.impl;
    opens com.lavyoung.marketforge.application.award.messaging;
}
