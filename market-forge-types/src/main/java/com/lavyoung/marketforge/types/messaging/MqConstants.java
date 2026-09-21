package com.lavyoung.marketforge.types.messaging;

import java.time.Duration;

/**
 *
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/21
 */
public final class MqConstants {

    private MqConstants() {
    }

    /**
     * 幂等键前缀，用于区分"消息已消费"标记与其他缓存用途。
     */
    public static final String CONSUMED_KEY_PREFIX = "mq:consumed:";

    /**
     * 等待 broker 确认的最长时间。
     */
    public static final Duration CONFIRM_TIMEOUT = Duration.ofSeconds(5);

    public static final String EXCHANGE = "market-forge.exchange";
    public static final String DLX = "market-forge.dlx";
    public static final String DLQ = "market-forge.dlq";

    public static final String AWARD_STOCK_DEDUCT_QUEUE = "award.stock.deduct.queue";
    public static final String AWARD_STOCK_DEDUCT_ROUTING_KEY = "award.stock.deduct";
}
