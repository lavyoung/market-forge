package com.lavyoung.marketforge.infrastructure.persistent.elasticsearch.document;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

/**
 * Elasticsearch 用户中奖记录文档。
 *
 * <p>字段与 {@code market_forge_user_award_record} 索引的
 * {@code _source} 保持一致。该类型仅属于 Infrastructure，不得向应用层暴露。</p>
 *
 * <p>{@code order_id} 已被 Canal Adapter 用作 Elasticsearch 文档
 * {@code _id}，不会出现在 {@code _source} 中，因此本对象不声明该字段。</p>
 *
 * @param id             原分库表自增标识
 * @param sourceDatabase 数据来源物理库
 * @param userId         用户标识
 * @param activityId     活动标识
 * @param strategyId     抽奖策略标识
 * @param awardId        奖品标识
 * @param awardTitle     奖品标题
 * @param awardTime      带时区的中奖时间
 * @param awardState     发奖状态
 * @param createTime     记录创建时间
 * @param updateTime     记录更新时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/16
 */
public record UserAwardRecordDocument(
        @JsonProperty("id")
        Long id,
        @JsonProperty("source_database")
        String sourceDatabase,
        @JsonProperty("user_id")
        String userId,
        @JsonProperty("activity_id")
        Long activityId,
        @JsonProperty("strategy_id")
        Long strategyId,
        @JsonProperty("award_id")
        Long awardId,
        @JsonProperty("award_title")
        String awardTitle,
        @JsonProperty("award_time")
        OffsetDateTime awardTime,
        @JsonProperty("award_state")
        String awardState,
        @JsonProperty("create_time")
        OffsetDateTime createTime,
        @JsonProperty("update_time")
        OffsetDateTime updateTime
) {
}
