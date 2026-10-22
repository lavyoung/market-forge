package com.lavyoung.marketforge.api.activity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 创建活动定义请求。
 *
 * @param activityName     活动名称
 * @param activityDesc     活动说明
 * @param beginDateTime    开始时间
 * @param endDateTime      结束时间
 * @param stockCount       库存总量
 * @param activityCountId  次数配置标识
 * @param strategyId       抽奖策略标识
 * @param awardUnlockScope 奖品解锁统计范围
 */
@Schema(description = "创建活动定义请求")
public record CreateActivityDefinitionRequest(
        @NotBlank(message = "活动名称不能为空") @Size(max = 64, message = "活动名称长度不能超过 64 个字符")
        @Schema(description = "活动名称", example = "双十一抽奖", requiredMode = Schema.RequiredMode.REQUIRED)
        String activityName,
        @NotBlank(message = "活动说明不能为空") @Size(max = 256, message = "活动说明长度不能超过 256 个字符")
        @Schema(description = "活动说明", example = "双十一促销活动", requiredMode = Schema.RequiredMode.REQUIRED)
        String activityDesc,
        @NotNull(message = "活动开始时间不能为空")
        @Schema(description = "开始时间", example = "2026-11-01T00:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime beginDateTime,
        @NotNull(message = "活动结束时间不能为空")
        @Schema(description = "结束时间", example = "2026-11-30T23:59:59", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDateTime endDateTime,
        @Positive(message = "活动库存必须为正数")
        @Schema(description = "库存总量", example = "10000", requiredMode = Schema.RequiredMode.REQUIRED)
        int stockCount,
        @NotNull(message = "活动次数配置不能为空") @Positive(message = "活动次数配置标识必须为正数")
        @Schema(description = "活动次数配置标识", example = "100301", requiredMode = Schema.RequiredMode.REQUIRED)
        Long activityCountId,
        @NotNull(message = "抽奖策略不能为空") @Positive(message = "抽奖策略标识必须为正数")
        @Schema(description = "抽奖策略标识", example = "100001", requiredMode = Schema.RequiredMode.REQUIRED)
        Long strategyId,
        @NotBlank(message = "奖品解锁统计范围不能为空")
        @Pattern(regexp = "^(day|total)$", message = "奖品解锁统计范围不合法")
        @Schema(description = "奖品解锁统计范围", allowableValues = {"day", "total"}, requiredMode = Schema.RequiredMode.REQUIRED)
        String awardUnlockScope
) implements Serializable {
}
