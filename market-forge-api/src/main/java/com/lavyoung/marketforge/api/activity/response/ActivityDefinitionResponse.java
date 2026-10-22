package com.lavyoung.marketforge.api.activity.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动定义运营响应。
 *
 * <p>活动、次数配置和策略标识均以字符串返回，避免浏览器使用 JavaScript number
 * 解析十八位业务标识时发生精度丢失。</p>
 *
 * @param activityId        活动业务标识
 * @param activityName      活动名称
 * @param activityDesc      活动说明
 * @param beginDateTime     活动开始时间
 * @param endDateTime       活动结束时间
 * @param stockCount        活动库存总量
 * @param stockCountSurplus 活动剩余库存
 * @param activityCountId   活动次数配置标识
 * @param strategyId        抽奖策略标识
 * @param awardUnlockScope  奖品解锁次数统计范围
 * @param state             活动生命周期状态
 * @param version           乐观锁版本
 * @param createdBy         创建人
 * @param updatedBy         最后修改人
 * @param createTime        创建时间
 * @param updateTime        最后修改时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Schema(description = "活动定义")
public record ActivityDefinitionResponse(
        @Schema(description = "活动业务标识", example = "100301", type = "string")
        String activityId,
        @Schema(description = "活动名称", example = "九宫格活动抽奖")
        String activityName,
        @Schema(description = "活动说明", example = "九宫格抽奖活动演示数据")
        String activityDesc,
        @Schema(description = "活动开始时间", example = "2026-01-01T00:00:00", type = "string", format = "date-time")
        LocalDateTime beginDateTime,
        @Schema(description = "活动结束时间", example = "2036-12-31T23:59:59", type = "string", format = "date-time")
        LocalDateTime endDateTime,
        @Schema(description = "活动库存总量", example = "100000")
        int stockCount,
        @Schema(description = "活动剩余库存", example = "99999")
        int stockCountSurplus,
        @Schema(description = "活动次数配置标识", example = "100301", type = "string")
        String activityCountId,
        @Schema(description = "抽奖策略标识", example = "900901001", type = "string")
        String strategyId,
        @Schema(description = "奖品解锁次数统计范围", example = "total", allowableValues = {"day", "total"})
        String awardUnlockScope,
        @Schema(description = "活动生命周期状态", example = "open", allowableValues = {"draft", "open", "close"})
        String state,
        @Schema(description = "乐观锁版本", example = "1")
        int version,
        @Schema(description = "创建人", example = "admin")
        String createdBy,
        @Schema(description = "最后修改人", example = "operator")
        String updatedBy,
        @Schema(description = "创建时间", example = "2026-10-22T10:00:00", type = "string", format = "date-time")
        LocalDateTime createTime,
        @Schema(description = "最后修改时间", example = "2026-10-22T10:30:00", type = "string", format = "date-time")
        LocalDateTime updateTime
) implements Serializable {
}
