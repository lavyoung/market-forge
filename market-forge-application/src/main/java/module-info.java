/**
 * Market Forge 应用层模块。
 * <p>
 * 负责用例编排、事务边界与领域模型转换，不包含 HTTP、RPC、消息队列等传输协议细节。
 */
module market.forge.application {
    requires market.forge.domain;
    requires org.slf4j;
    requires spring.context;
    requires static lombok;
    exports com.lavyoung.marketforge.application.strategy.model;
    exports com.lavyoung.marketforge.application.strategy.service;

    opens com.lavyoung.marketforge.application.strategy.service;
    exports com.lavyoung.marketforge.application.strategy.service.impl;
    opens com.lavyoung.marketforge.application.strategy.service.impl;
}
