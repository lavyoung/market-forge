package com.lavyoung.marketforge.types.common;

/**
 * 跨模块共享常量。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
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
     * 业务单号前缀。
     * <p>
     * 仅表达业务来源，不承担全局唯一性；完整单号仍需由 ID 生成器或外部业务号生成。
     */
    public interface BusinessNoPrefix {

        /**
         * 抽奖活动订单前缀。
         */
        String RAFFLE_ORDER_PREFIX = "ROP";
    }

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

        /**
         * 策略奖品 Redis 库存计数器键前缀。
         */
        public static final String STRATEGY_AWARD_STOCK = "strategy_award_stock:";

        /**
         * 策略奖品库存异步同步队列键。
         */
        public static final String STRATEGY_AWARD_STOCK_QUEUE = "strategy_award_stock_queue";

        /**
         * 活动 SKU 库存异步同步队列键。
         */
        public static final String ACTIVITY_SKU_STOCK_QUEUE = "activity_sku_stock_queue";

        /**
         * 活动详情缓存键前缀。
         */
        public static final String ACTIVITY_DETAIL_KEY = "activity_detail_key:";

        /**
         * 活动 SKU 库存缓存计数器键前缀。
         */
        public static final String ACTIVITY_SKU_STOCK_COUNT_KEY = "activity_sku_stock_count_key:";

        /**
         * 活动 SKU 次数配置查询缓存键前缀。
         */
        public static final String ACTIVITY_SKU_COUNT_QUERY_KEY = "activity_sku_count_query:";
    }
}
