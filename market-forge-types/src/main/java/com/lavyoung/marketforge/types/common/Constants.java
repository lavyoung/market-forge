package com.lavyoung.marketforge.types.common;

/**
 * 跨模块共享常量。
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 */
public class Constants {

    /**
     * 路径式配置值的元素分隔符。
     */
    public static final String SPLIT = "/";

    /**
     * 键值配置的键值分隔符。
     */
    public static final String COLON = ":";

    /**
     * 空字符串。
     */
    public static final String SPACE = "";

    /**
     * 多组配置之间的分隔符。
     */
    public static final String SEMICOLON = ";";

    /**
     * 复合键各组成部分之间的下划线分隔符。
     */
    public static final String UNDERLINE = "_";


    /**
     * 抽奖策略相关的 Redis 键前缀。
     */
    public static class RedisKeys {

        /**
         * 策略奖品列表的缓存键前缀。
         */
        public static final String STRATEGY_AWARD_KEY = "strategy_award_key:";

        /**
         * 策略基础信息缓存键前缀。
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
