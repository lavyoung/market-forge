package com.lavyoung.marketforge.infrastructure.messaging.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 客户端配置。
 * <p>
 * 统一声明 JSON 消息转换器和 {@link RabbitTemplate}，并开启 mandatory 投递诊断，
 * 让发布端可以感知 broker 确认和不可路由消息。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
@Slf4j
@Configuration
public class RabbitMqConfig {

    /**
     * 创建 RabbitMQ JSON 消息转换器。
     *
     * @param objectMapper Spring Boot 统一配置的 Jackson 对象映射器
     * @return Jackson 消息转换器
     */
    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * 创建带发布确认和退回诊断的 RabbitTemplate。
     *
     * @param connectionFactory RabbitMQ 连接工厂
     * @param converter         JSON 消息转换器
     * @return RabbitMQ 操作模板
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        if (connectionFactory instanceof CachingConnectionFactory ccf) {
            log.info("RabbitMQ 连接工厂={} 发布确认={} 发布退回={}",
                    connectionFactory.getClass().getSimpleName(),
                    ccf.isPublisherConfirms(), ccf.isPublisherReturns());
        } else {
            log.warn("RabbitMQ 连接工厂不是 CachingConnectionFactory，实际={}", connectionFactory.getClass().getName());
        }
        return createTemplate(connectionFactory, converter);
    }

    /**
     * 组装 RabbitTemplate 的消息转换、mandatory 和诊断回调。
     *
     * @param connectionFactory RabbitMQ 连接工厂
     * @param converter         JSON 消息转换器
     * @return RabbitMQ 操作模板
     */
    private static RabbitTemplate createTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        template.setMandatory(true);
        template.setConfirmCallback((correlationData, ack, cause) ->
                log.debug("MQ 诊断-收到 broker 确认 ack={} messageId={} cause={}", ack, correlationData == null ? null : correlationData.getId(), cause)
        );
        template.setReturnsCallback(returned -> {
            log.error(
                    "消息无法路由到队列 exchange={} routingKey={} replyText={}",
                    returned.getExchange(), returned.getRoutingKey(), returned.getReplyText());
        });
        return template;
    }
}
