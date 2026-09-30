package com.lavyoung.marketforge.types.messaging;

import java.time.Duration;

/**
 * MQ 拓扑和路由常量。
 * <p>
 * 集中维护交换机、队列、路由键和发送确认等待时间，避免各消息发布/消费适配器散落硬编码。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
public final class MqConstants {

    private MqConstants() {
    }

    /**
     * 等待 broker 确认的最长时间。
     */
    public static final Duration CONFIRM_TIMEOUT = Duration.ofSeconds(5);

    /**
     * 默认业务主交换机。
     */
    public static final String EXCHANGE = "market-forge.exchange";

    /**
     * 公共死信交换机。
     */
    public static final String DLX = "market-forge.dlx";

    /**
     * 公共死信队列。
     */
    public static final String DLQ = "market-forge.dlq";

    /**
     * 奖品库存扣减队列。
     */
    public static final String AWARD_STOCK_DEDUCT_QUEUE = "award.stock.deduct.queue";

    /**
     * 奖品库存扣减路由键。
     */
    public static final String AWARD_STOCK_DEDUCT_ROUTING_KEY = "award.stock.deduct";

    /**
     * 活动 SKU 库存扣减队列。
     */
    public static final String SKU_STOCK_DEDUCT_QUEUE = "activity.sku.stock.deduct.queue";

    /**
     * 活动 SKU 库存扣减路由键。
     */
    public static final String SKU_STOCK_DEDUCT_ROUTING_KEY = "activity.sku.stock.deduct";

    /**
     * 活动 SKU 库存清零队列。
     */
    public static final String SKU_STOCK_ZERO_QUEUE = "activity.sku.stock.zero.queue";

    /**
     * 活动 SKU 库存清零路由键。
     */
    public static final String SKU_STOCK_ZERO_ROUTING_KEY = "activity.sku.stock.zero";

    /**
     * 用户中奖记录和发奖流程主交换机。
     */
    public static final String USER_AWARD_EXCHANGE = "market-forge.user.award.exchange";

    /**
     * 用户中奖记录创建事件路由键。
     */
    public static final String USER_AWARD_SEND_ROUTE_KEY = "user.award.send.key";

    /**
     * 用户中奖记录创建事件队列。
     */
    public static final String USER_AWARD_SEND_QUEUE = "user.award.send.queue";
}
