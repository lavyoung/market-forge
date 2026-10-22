package com.lavyoung.marketforge.api.activity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.io.Serializable;

/**
 * 活动展台运营分页查询请求。
 *
 * <p>渠道、来源和状态均为可选的精确匹配条件。分页大小被限制为最多 100 条，
 * 防止后台误操作形成无边界数据库查询。</p>
 *
 * @param channel    投放渠道；为空时不限制
 * @param source     流量来源；为空时不限制
 * @param state      生命周期状态；为空时不限制
 * @param pageNumber 页码，从 1 开始
 * @param pageSize   每页记录数，范围为 1 至 100
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Schema(description = "活动展台运营分页查询条件")
public record ActivityStageSearchRequest(
        @Schema(description = "投放渠道，精确匹配", example = "web")
        @Pattern(regexp = "^(?:[a-z][a-z0-9_-]{0,31})?$", message = "投放渠道格式不合法")
        String channel,
        @Schema(description = "流量来源，精确匹配", example = "official")
        @Pattern(regexp = "^(?:[a-z][a-z0-9_-]{0,31})?$", message = "流量来源格式不合法")
        String source,
        @Schema(description = "生命周期状态", example = "active", allowableValues = {"create", "active", "expire"})
        @Pattern(regexp = "^(?:create|active|expire)?$", message = "展台生命周期状态不合法")
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
