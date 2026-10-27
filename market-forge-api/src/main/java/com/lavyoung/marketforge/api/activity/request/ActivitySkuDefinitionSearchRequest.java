package com.lavyoung.marketforge.api.activity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.io.Serializable;

/**
 * 活动 SKU 分页查询请求。
 *
 * @param sku             SKU 业务标识；为空时不限制
 * @param activityId      所属活动标识；为空时不限制
 * @param activityCountId 次数配置标识；为空时不限制
 * @param pageNumber      页码，从 1 开始
 * @param pageSize        每页记录数，范围为 1 至 100
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
@Schema(description = "活动 SKU 分页查询条件")
public record ActivitySkuDefinitionSearchRequest(
        @Schema(description = "SKU 业务标识", example = "900000000000000001", type = "string")
        @Pattern(regexp = "^(?:[1-9][0-9]{0,17})?$", message = "SKU 格式不合法")
        String sku,
        @Schema(description = "活动业务标识", example = "100000000000000001", type = "string")
        @Pattern(regexp = "^(?:[1-9][0-9]{0,17})?$", message = "活动标识格式不合法")
        String activityId,
        @Schema(description = "次数配置业务标识", example = "300000000000000001", type = "string")
        @Pattern(regexp = "^(?:[1-9][0-9]{0,17})?$", message = "次数配置标识格式不合法")
        String activityCountId,
        @Schema(description = "页码", example = "1", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "页码不能小于 1")
        int pageNumber,
        @Schema(description = "每页记录数", example = "20", minimum = "1", maximum = "100", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "每页记录数不能小于 1")
        @Max(value = 100, message = "每页记录数不能超过 100")
        int pageSize
) implements Serializable {
}
