package com.lavyoung.marketforge.api.award.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 奖品定义运营分页查询请求。
 *
 * <p>用于运营后台按照奖品编码、名称、发奖类型和生命周期状态组合查询奖品定义。
 * 所有过滤条件均为可选项，分页参数必须满足规定范围。</p>
 *
 * <p>奖品编码使用精确匹配，奖品名称使用前缀匹配。前缀匹配能够利用
 * {@code award_name} 索引的最左前缀，避免无边界的包含式模糊查询。</p>
 *
 * @param awardCode  奖品业务编码；为空时不限制
 * @param awardName  奖品名称前缀；为空时不限制
 * @param awardType  发奖处理器类型；为空时不限制
 * @param state      生命周期状态；为空时不限制
 * @param pageNumber 页码，从 1 开始
 * @param pageSize   每页记录数，范围为 1 至 100
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Schema(description = "奖品定义运营分页查询条件")
public record AwardDefinitionSearchRequest(
        @Schema(description = "奖品业务编码，精确匹配", example = "credit_100", maxLength = 64)
        @Pattern(regexp = "^(?:[a-z][a-z0-9_-]{1,63})?$", message = "奖品业务编码格式不合法")
        String awardCode,

        @Schema(description = "奖品名称前缀", example = "积分", maxLength = 128)
        @Size(max = 128, message = "奖品名称长度不能超过 128 个字符")
        String awardName,

        @Schema(description = "发奖处理器类型", example = "internal_credit", maxLength = 32)
        @Pattern(regexp = "^(?:[a-z][a-z0-9_]{1,31})?$", message = "发奖类型格式不合法")
        String awardType,

        @Schema(description = "生命周期状态", example = "active", allowableValues = {"draft", "active", "disabled"})
        @Pattern(regexp = "^(?:draft|active|disabled)?$", message = "奖品生命周期状态不合法")
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
