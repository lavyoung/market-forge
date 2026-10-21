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

        /**
         * 用户返利行为订单前缀
         */
        String REBATE_ORDER_PREFIX = "BRO";
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
         * 二分查找算法累计概率区间表的缓存键前缀。
         *
         * <p>缓存内容为按累计概率上界升序排列的奖品区间列表。</p>
         */
        public static final String STRATEGY_PROBABILITY_RANGE_TABLE_KEY = "strategy_probability_range_table_key:";

        /**
         * Alias Method 别名概率表的缓存键前缀。
         *
         * <p>缓存值包含奖品列、原奖品接受概率和候补别名列，作为一次策略
         * 装配生成的完整不可变快照。</p>
         */
        public static final String STRATEGY_ALIAS_TABLE_KEY = "strategy_alias_table_key:";

        /**
         * 策略概率算法类型的缓存键前缀。
         */
        public static final String STRATEGY_PROBABILITY_ALGORITHM_KEY = "strategy_probability_algorithm_key:";

        /**
         * 概率快照版本计数器的 Redis Key 前缀。
         *
         * <p>每个装配键拥有独立计数器，通过 Redis 原子递增生成单调版本。
         * 版本允许不连续，例如装配失败可以消耗版本但不发布。</p>
         */
        public static final String STRATEGY_PROBABILITY_SNAPSHOT_VERSION_KEY = "strategy_probability_snapshot_version_key:";

        /**
         * 当前生效概率快照指针的 Redis Key 前缀。
         *
         * <p>缓存值同时包含装配键、算法类型和快照版本，保证算法选择与概率数据
         * 版本能够作为一个整体发布。</p>
         */
        public static final String STRATEGY_PROBABILITY_SNAPSHOT_POINTER_KEY = "strategy_probability_snapshot_pointer_key:";

        /**
         * 概率快照发布锁的 Redis Key 前缀。
         *
         * <p>锁只保护低频装配阶段的指针比较和替换，不参与高频抽奖派发。</p>
         */
        public static final String STRATEGY_PROBABILITY_SNAPSHOT_PUBLISH_LOCK_KEY = "strategy_probability_snapshot_publish_lock_key:";

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
         * 活动 SKU 基础配置缓存键前缀。
         */
        public static final String ACTIVITY_SKU_KEY = "activity_sku_key:";

        /**
         * 活动 SKU 库存缓存计数器键前缀。
         */
        public static final String ACTIVITY_SKU_STOCK_COUNT_KEY = "activity_sku_stock_count_key:";

        /**
         * 活动次数配置缓存键前缀。
         */
        public static final String ACTIVITY_COUNT_KEY = "activity_count_key:";
    }
}
