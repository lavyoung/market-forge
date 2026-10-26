package com.lavyoung.marketforge.api.activity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 活动次数配置分页查询请求。
 *
 * @param activityCountId   活动次数配置业务标识；为空时不限制
 * @param activityCountName 配置名称前缀；为空时不限制
 * @param pageNumber        页码，从 1 开始
 * @param pageSize          每页记录数，范围为 1 至 100
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
@Schema(description = "活动次数配置分页查询条件")
public record ActivityCountDefinitionSearchRequest(
        @Schema(description = "活动次数配置业务标识", example = "100301", type = "string")
        @Pattern(regexp = "^(?:[1-9][0-9]{0,18})?$", message = "活动次数配置标识格式不合法")
        String activityCountId,
        @Schema(description = "配置名称前缀", example = "签到")
        @Size(max = 64, message = "配置名称不能超过 64 个字符")
        String activityCountName,
        @Schema(description = "页码", example = "1", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "页码不能小于 1")
        int pageNumber,
        @Schema(description = "每页记录数", example = "20", minimum = "1", maximum = "100", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "每页记录数不能小于 1")
        @Max(value = 100, message = "每页记录数不能超过 100")
        int pageSize
) implements Serializable {
}
