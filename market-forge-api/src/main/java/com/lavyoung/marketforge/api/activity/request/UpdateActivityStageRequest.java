package com.lavyoung.marketforge.api.activity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.io.Serializable;

/**
 * 修改待上架活动展台请求。
 *
 * <p>只有 {@code create} 状态允许修改。{@code expectedVersion} 用于乐观锁校验，
 * 防止两个运营人员互相覆盖配置。</p>
 *
 * @param channel         投放渠道
 * @param source          流量来源
 * @param activityId      待上架活动标识
 * @param expectedVersion 客户端读取到的版本
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Schema(description = "修改待上架活动展台请求")
public record UpdateActivityStageRequest(
        @Schema(description = "投放渠道", example = "web", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "投放渠道不能为空")
        @Pattern(regexp = "^[a-z][a-z0-9_-]{0,31}$", message = "投放渠道格式不合法")
        String channel,
        @Schema(description = "流量来源", example = "official", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "流量来源不能为空")
        @Pattern(regexp = "^[a-z][a-z0-9_-]{0,31}$", message = "流量来源格式不合法")
        String source,
        @Schema(description = "待上架活动标识", example = "100301", type = "string", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "活动标识不能为空")
        @Pattern(regexp = "^[1-9][0-9]{0,18}$", message = "活动标识格式不合法")
        String activityId,
        @Schema(description = "乐观锁版本", example = "1", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "乐观锁版本不能小于 1")
        int expectedVersion
) implements Serializable {
}
