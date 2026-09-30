package com.lavyoung.marketforge.types.messaging;

/**
 * 集成消息发布端口。
 * <p>
 * 应用层通过该端口发布集成事件，具体 MQ 产品、消息信封格式和 broker 确认策略由基础设施实现。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
public interface MessagePublisher {

    /**
     * 发布一条集成事件。
     *
     * @param event 待发布的集成事件
     * @throws RuntimeException 消息发送、路由或 broker 确认失败时抛出
     */
    void publish(IntegrationEvent event);

}
