package com.lavyoung.marketforge.api.activity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 创建活动次数配置请求。
 *
 * @param activityCountName 配置名称
 * @param totalCount        一次入账增加的总抽奖次数
 * @param dayCount          一次入账增加的日抽奖次数
 * @param monthCount        一次入账增加的月抽奖次数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
@Schema(description = "创建活动次数配置请求")
public record CreateActivityCountDefinitionRequest(
        @Schema(description = "配置名称", example = "每日签到次数包", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "配置名称不能为空")
        @Size(max = 64, message = "配置名称不能超过 64 个字符")
        String activityCountName,
        @Schema(description = "总入账次数", example = "100", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "总次数不能小于 1")
        int totalCount,
        @Schema(description = "日入账次数", example = "10", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "日次数不能小于 1")
        int dayCount,
        @Schema(description = "月入账次数", example = "50", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "月次数不能小于 1")
        int monthCount
) implements Serializable {
}
