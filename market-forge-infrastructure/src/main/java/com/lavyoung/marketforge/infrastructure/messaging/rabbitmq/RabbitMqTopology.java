package com.lavyoung.marketforge.infrastructure.messaging.rabbitmq;

import com.lavyoung.marketforge.types.messaging.MqConstants;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Market Forge RabbitMQ 拓扑配置。
 *
 * <p>主交换机按照业务 routing key 将消息路由到三个业务队列；业务消费失败且不重新入队时，
 * RabbitMQ 再将消息发送到死信交换机，最终汇聚到公共死信队列。</p>
 *
 * <pre>
 * Producer
 *    |
 *    v
 * market-forge.exchange (TopicExchange)
 *    |-- award.stock.deduct -----------&gt; award.stock.deduct.queue
 *    |                                      |
 *    |                                      | reject / retry exhausted
 *    |                                      v
 *    |                                  market-forge.dlx
 *    |                                      |
 *    |                                      | award.stock.deduct
 *    |                                      v
 *    |                                  market-forge.dlq
 *    |
 *    |-- activity.sku.stock.deduct -----&gt; activity.sku.stock.deduct.queue
 *    |                                      |
 *    |                                      | reject / retry exhausted
 *    |                                      v
 *    |                                  market-forge.dlx
 *    |                                      |
 *    |                                      | activity.sku.stock.deduct
 *    |                                      v
 *    |                                  market-forge.dlq
 *    |
 *    `-- activity.sku.stock.zero -------&gt; activity.sku.stock.zero.queue
 *                                           |
 *                                           | reject / retry exhausted
 *                                           v
 *                                       market-forge.dlx
 *                                           |
 *                                           | activity.sku.stock.zero
 *                                           v
 *                                       market-forge.dlq
 * </pre>
 *
 * todo 所有失败消息应该区分一下
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
@Configuration
public class RabbitMqTopology {

    /**
     * 声明业务主交换机，路由方向为：消息发布者 -&gt; 主交换机 -&gt; routing key 对应的业务队列。
     *
     * @return 持久化 TopicExchange
     */
    @Bean
    public TopicExchange marketForgeExchange() {
        return new TopicExchange(MqConstants.EXCHANGE, true, false);
    }

    /**
     * 声明死信交换机，路由方向为：业务队列 -&gt; 死信交换机 -&gt; 公共死信队列。
     *
     * @return 持久化 DirectExchange
     */
    @Bean
    public DirectExchange marketForgeDlx() {
        return new DirectExchange(MqConstants.DLX, true, false);
    }

    /**
     * 声明公共死信队列，接收奖品库存扣减、SKU 库存扣减和 SKU 库存置零三类死信。
     *
     * @return 持久化公共死信队列
     */
    @Bean
    public Queue marketForgeDlq() {
        return QueueBuilder.durable(MqConstants.DLQ).build();
    }

    /**
     * 声明奖品库存扣减业务队列。
     *
     * <p>正常方向：主交换机 -&gt; 奖品库存扣减队列；失败方向：奖品库存扣减队列 -&gt; 死信交换机。</p>
     *
     * @return 带死信参数的奖品库存扣减队列
     */
    @Bean
    public Queue awardStockDeductQueue() {
        return QueueBuilder.durable(MqConstants.AWARD_STOCK_DEDUCT_QUEUE)
                .deadLetterExchange(MqConstants.DLX)
                .deadLetterRoutingKey(MqConstants.AWARD_STOCK_DEDUCT_ROUTING_KEY)
                .build();
    }

    /**
     * 绑定奖品库存扣减路由，方向为：主交换机 -&gt; 奖品库存扣减队列。
     *
     * @return 奖品库存扣减业务绑定
     */
    @Bean
    public Binding awardStockDeductBinding() {
        return BindingBuilder.bind(awardStockDeductQueue())
                .to(marketForgeExchange())
                .with(MqConstants.AWARD_STOCK_DEDUCT_ROUTING_KEY);
    }

    /**
     * 绑定奖品库存扣减死信路由，方向为：死信交换机 -&gt; 公共死信队列。
     *
     * @return 奖品库存扣减死信绑定
     */
    @Bean
    public Binding dlqBinding() {
        return BindingBuilder.bind(marketForgeDlq())
                .to(marketForgeDlx())
                .with(MqConstants.AWARD_STOCK_DEDUCT_ROUTING_KEY);
    }

    /**
     * 声明 SKU 库存扣减业务队列。
     *
     * <p>正常方向：主交换机 -&gt; SKU 库存扣减队列；失败方向：SKU 库存扣减队列 -&gt; 死信交换机。</p>
     *
     * @return 带死信参数的 SKU 库存扣减队列
     */
    @Bean
    public Queue skuStockDeductQueue() {
        return QueueBuilder.durable(MqConstants.SKU_STOCK_DEDUCT_QUEUE)
                .deadLetterExchange(MqConstants.DLX)
                .deadLetterRoutingKey(MqConstants.SKU_STOCK_DEDUCT_ROUTING_KEY)
                .build();
    }

    /**
     * 绑定 SKU 库存扣减路由，方向为：主交换机 -&gt; SKU 库存扣减队列。
     *
     * @return SKU 库存扣减业务绑定
     */
    @Bean
    public Binding skuStockDeductBinding() {
        return BindingBuilder.bind(skuStockDeductQueue())
                .to(marketForgeExchange())
                .with(MqConstants.SKU_STOCK_DEDUCT_ROUTING_KEY);
    }

    /**
     * 绑定 SKU 库存扣减死信路由，方向为：死信交换机 -&gt; 公共死信队列。
     *
     * @return SKU 库存扣减死信绑定
     */
    @Bean
    public Binding dlqBinding2() {
        return BindingBuilder.bind(marketForgeDlq())
                .to(marketForgeDlx())
                .with(MqConstants.SKU_STOCK_DEDUCT_ROUTING_KEY);
    }

    /**
     * 声明 SKU 库存置零业务队列。
     *
     * <p>正常方向：主交换机 -&gt; SKU 库存置零队列；失败方向：SKU 库存置零队列 -&gt; 死信交换机。</p>
     *
     * @return 带死信参数的 SKU 库存置零队列
     */
    @Bean
    public Queue skuStockZeroQueue() {
        return QueueBuilder.durable(MqConstants.SKU_STOCK_ZERO_QUEUE)
                .deadLetterExchange(MqConstants.DLX)
                .deadLetterRoutingKey(MqConstants.SKU_STOCK_ZERO_ROUTING_KEY)
                .build();
    }

    /**
     * 绑定 SKU 库存置零路由，方向为：主交换机 -&gt; SKU 库存置零队列。
     *
     * @return SKU 库存置零业务绑定
     */
    @Bean
    public Binding skuStockZeroBinding() {
        return BindingBuilder.bind(skuStockZeroQueue())
                .to(marketForgeExchange())
                .with(MqConstants.SKU_STOCK_ZERO_ROUTING_KEY);
    }

    /**
     * 绑定 SKU 库存置零死信路由，方向为：死信交换机 -&gt; 公共死信队列。
     *
     * @return SKU 库存置零死信绑定
     */
    @Bean
    public Binding dlqBinding3() {
        return BindingBuilder.bind(marketForgeDlq())
                .to(marketForgeDlx())
                .with(MqConstants.SKU_STOCK_ZERO_ROUTING_KEY);
    }
}
