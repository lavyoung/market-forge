package com.lavyoung.marketforge.api.activity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.io.Serializable;

/**
 * 活动定义运营分页查询请求。
 *
 * <p>支持按活动标识精确查询、活动名称前缀查询和生命周期状态查询。名称只允许前缀匹配，
 * 以便利用 {@code activity_name} 索引，避免后台查询退化为无边界全表扫描。</p>
 *
 * @param activityId   活动业务标识；为空时不限制
 * @param activityName 活动名称前缀；为空时不限制
 * @param state        活动生命周期状态；为空时不限制
 * @param pageNumber   页码，从 1 开始
 * @param pageSize     每页记录数，范围为 1 至 100
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Schema(description = "活动定义运营分页查询条件")
public record ActivityDefinitionSearchRequest(
        @Schema(description = "活动业务标识，精确匹配", example = "100301", type = "integer", format = "int64")
        @Positive(message = "活动标识必须为正数")
        Long activityId,

        @Schema(description = "活动名称前缀", example = "九宫格", maxLength = 64)
        @Size(max = 64, message = "活动名称长度不能超过 64 个字符")
        String activityName,

        @Schema(description = "活动生命周期状态", example = "open", allowableValues = {"draft", "open", "close"})
        @Pattern(regexp = "^(?:draft|open|close)?$", message = "活动生命周期状态不合法")
        String state,

        @Schema(description = "页码，从 1 开始", example = "1", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "页码不能小于 1")
        int pageNumber,

        @Schema(description = "每页记录数", example = "20", minimum = "1", maximum = "100", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "每页记录数不能小于 1")
        @Max(value = 100, message = "每页记录数不能超过 100")
        int pageSize
) implements Serializable {
}
