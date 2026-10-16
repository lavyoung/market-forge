package com.lavyoung.marketforge.application.award.model;

import java.time.LocalDateTime;

/**
 * 用户中奖记录搜索结果项。
 *
 * <p>该模型是应用层稳定的查询结果，不直接暴露 Elasticsearch 文档类型。
 * 后续即使更换索引字段、索引版本或搜索引擎，上层接口也不需要随之变化。</p>
 *
 * @param orderId    抽奖订单标识
 * @param userId     用户标识
 * @param activityId 活动标识
 * @param strategyId 策略标识
 * @param awardId    奖品标识
 * @param awardTitle 奖品标题
 * @param awardState 发奖状态
 * @param awardTime  中奖时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/15
 */
public record UserAwardRecordSearchItem(
        String orderId,
        String userId,
        Long activityId,
        Long strategyId,
        Long awardId,
        String awardTitle,
        String awardState,
        LocalDateTime awardTime
) {
}
