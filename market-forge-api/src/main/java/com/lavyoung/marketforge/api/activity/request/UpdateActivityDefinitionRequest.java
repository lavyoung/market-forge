package com.lavyoung.marketforge.api.activity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 修改活动草稿请求。
 *
 * @param activityName     活动名称
 * @param activityDesc     活动说明
 * @param beginDateTime    开始时间
 * @param endDateTime      结束时间
 * @param stockCount       库存总量
 * @param activityCountId  次数配置标识
 * @param strategyId       抽奖策略标识
 * @param awardUnlockScope 奖品解锁统计范围
 * @param expectedVersion  页面读取到的乐观锁版本
 */
@Schema(description = "修改活动草稿请求")
public record UpdateActivityDefinitionRequest(
        @NotBlank(message = "活动名称不能为空") @Size(max = 64, message = "活动名称长度不能超过 64 个字符") String activityName,
        @NotBlank(message = "活动说明不能为空") @Size(max = 256, message = "活动说明长度不能超过 256 个字符") String activityDesc,
        @NotNull(message = "活动开始时间不能为空") LocalDateTime beginDateTime,
        @NotNull(message = "活动结束时间不能为空") LocalDateTime endDateTime,
        @Positive(message = "活动库存必须为正数") int stockCount,
        @NotNull(message = "活动次数配置不能为空") @Positive(message = "活动次数配置标识必须为正数") Long activityCountId,
        @NotNull(message = "抽奖策略不能为空") @Positive(message = "抽奖策略标识必须为正数") Long strategyId,
        @NotBlank(message = "奖品解锁统计范围不能为空")
        @Pattern(regexp = "^(day|total)$", message = "奖品解锁统计范围不合法") String awardUnlockScope,
        @Min(value = 1, message = "期望版本不能小于 1") int expectedVersion
) implements Serializable {
}
