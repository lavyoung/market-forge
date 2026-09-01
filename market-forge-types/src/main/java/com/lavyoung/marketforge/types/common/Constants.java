package com.lavyoung.marketforge.types.common;

/**
 * 常量
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 */
public class Constants {


    /**
     * 抽奖策略相关的 Redis 键前缀。
     */
    public static class RedisKeys {

        /**
         * 策略奖品列表的缓存键前缀。
         */
        public static final String STRATEGY_AWARD_KEY = "strategy_award_key:";

        /**
         *
         */
        public static final String STRATEGY_KEY = "strategy_key:";

        /**
         * 策略概率范围的缓存键前缀。
         */
        public static final String STRATEGY_RATE_RANGE_KEY = "strategy_rate_range_key:";

        /**
         * 策略概率查找表的缓存键前缀。
         */
        public static final String STRATEGY_RATE_TABLE_KEY = "strategy_rate_table_key:";
    }
}
