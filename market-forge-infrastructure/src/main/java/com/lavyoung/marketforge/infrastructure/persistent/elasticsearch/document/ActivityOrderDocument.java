package com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.document;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * Elasticsearch 活动订单文档。
 *
 * <p>字段与 {@code market_forge_raffle_activity_order} 索引的
 * {@code _source} 保持一致。该类型只属于 Infrastructure 层，
 * 不得作为应用结果或接口响应向外暴露。</p>
 *
 * <p>全业务唯一的 {@code order_id} 已由 Canal Adapter 写入 Elasticsearch
 * 文档 {@code _id}，不会出现在 {@code _source} 中，因此本对象不声明该字段。</p>
 *
 * @param id             原分库表自增主键
 * @param sourceDatabase 数据来源物理库
 * @param userId         用户标识
 * @param activityId     活动标识
 * @param sku            活动商品 SKU
 * @param activityName   下单时保存的活动名称快照
 * @param strategyId     抽奖策略标识
 * @param orderTime      业务下单时间
 * @param totalCount     总抽奖次数
 * @param dayCount       日抽奖次数
 * @param monthCount     月抽奖次数
 * @param state          订单状态
 * @param outBusinessNo  外部业务幂等号
 * @param createTime     数据创建时间
 * @param updateTime     数据最后更新时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/18
 */
public record ActivityOrderDocument(
        @JsonProperty("id") Long id,
        @JsonProperty("source_database") String sourceDatabase,
        @JsonProperty("user_id") String userId,
        @JsonProperty("activity_id") Long activityId,
        @JsonProperty("sku") Long sku,
        @JsonProperty("activity_name") String activityName,
        @JsonProperty("strategy_id") Long strategyId,
        @JsonProperty("order_time") OffsetDateTime orderTime,
        @JsonProperty("total_count") Integer totalCount,
        @JsonProperty("day_count") Integer dayCount,
        @JsonProperty("month_count") Integer monthCount,
        @JsonProperty("state") String state,
        @JsonProperty("out_business_no") String outBusinessNo,
        @JsonProperty("create_time") OffsetDateTime createTime,
        @JsonProperty("update_time") OffsetDateTime updateTime
) {
}
