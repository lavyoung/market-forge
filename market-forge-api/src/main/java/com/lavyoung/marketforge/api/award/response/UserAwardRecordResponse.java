package com.lavyoung.marketforge.api.award.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户中奖记录响应。
 *
 * <p>该对象描述运营查询接口返回的一条中奖记录，不暴露 Elasticsearch 文档结构、
 * 分库来源、自增主键或其他基础设施字段。</p>
 *
 * @param orderId    全局唯一的抽奖订单标识
 * @param userId     用户标识
 * @param activityId 活动标识
 * @param strategyId 抽奖策略标识
 * @param awardId    奖品标识
 * @param awardTitle 奖品标题
 * @param awardState 发奖状态
 * @param awardTime  中奖时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/16
 */
@Schema(description = "用户中奖记录")
public record UserAwardRecordResponse(
        @Schema(description = "抽奖订单标识", example = "ROPmv8av9hj0002z38fvn2949wettup")
        String orderId,

        @Schema(description = "用户标识", example = "user-001")
        String userId,

        @Schema(description = "活动标识", example = "100301")
        Long activityId,

        @Schema(description = "抽奖策略标识", example = "100006")
        Long strategyId,

        @Schema(description = "奖品标识", example = "101")
        Long awardId,

        @Schema(description = "奖品标题", example = "用户积分")
        String awardTitle,

        @Schema(
                description = "发奖状态",
                example = "completed",
                allowableValues = {
                        "create",
                        "wait_claim",
                        "granting",
                        "grant_failed",
                        "completed"
                }
        )
        String awardState,

        @Schema(
                description = "中奖时间",
                example = "2026-10-16T09:30:00",
                type = "string",
                format = "date-time"
        )
        LocalDateTime awardTime
) implements Serializable {
}
