package com.lavyoung.marketforge.infrastructure.messaging.rabbitmq;

import com.lavyoung.marketforge.types.messaging.MqConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
@Configuration
public class RabbitMqTopology {

    @Bean
    public TopicExchange marketForgeExchange() {
        return new TopicExchange(MqConstants.EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange marketForgeDlx() {
        return new DirectExchange(MqConstants.DLX, true, false);
    }

    @Bean
    public Queue marketForgeDlq() {
        return QueueBuilder.durable(MqConstants.DLQ).build();
    }


    // ===== 新增 1：业务队列（带死信参数）=====
    @Bean
    public Queue awardStockDeductQueue() {
        return QueueBuilder.durable(MqConstants.AWARD_STOCK_DEDUCT_QUEUE)
                .deadLetterExchange(MqConstants.DLX)
                .deadLetterRoutingKey(MqConstants.AWARD_STOCK_DEDUCT_ROUTING_KEY)
                .build();
    }

    // ===== 新增 2：业务队列绑定到主交换机 =====
    @Bean
    public Binding awardStockDeductBinding() {
        return BindingBuilder.bind(awardStockDeductQueue())
                .to(marketForgeExchange())
                .with(MqConstants.AWARD_STOCK_DEDUCT_ROUTING_KEY);
    }

    // ===== 新增 3：死信队列绑定到死信交换机 =====
    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(marketForgeDlq())
                .to(marketForgeDlx())
                .with(MqConstants.AWARD_STOCK_DEDUCT_ROUTING_KEY);
    }


}
