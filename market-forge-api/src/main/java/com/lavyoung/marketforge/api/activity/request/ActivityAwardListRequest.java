package com.lavyoung.marketforge.api.activity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;

/**
 * 活动奖品列表查询请求。
 * <p>
 * 这个请求不是按 strategyId 查询，而是按用户和活动查询。
 * 因为前端活动页需要知道“当前用户”在“当前活动”下看到的奖品解锁状态。
 *
 * @param userId     用户标识
 * @param activityId 抽奖活动标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/01
 */
@Schema(name = "ActivityAwardListRequest", description = "活动奖品列表查询请求")
public record ActivityAwardListRequest(
        @Schema(
                description = "用户标识",
                example = "user-001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "用户标识不能为空")
        String userId,

        @Schema(
                description = "抽奖活动标识",
                example = "100301",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "1"
        )
        @NotNull(message = "活动标识不能为空")
        @Positive(message = "活动标识必须大于零")
        Long activityId
) implements Serializable {

}
