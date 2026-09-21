package com.lavyoung.marketforge.types.messaging;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
public interface MessagePublisher {

    void publish(IntegrationEvent event);

}
