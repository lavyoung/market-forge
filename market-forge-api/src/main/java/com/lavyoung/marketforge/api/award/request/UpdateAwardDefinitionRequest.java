package com.lavyoung.marketforge.api.award.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * 修改草稿奖品定义请求。
 *
 * <p>奖品标识由 HTTP 路径提供，操作人由当前登录会话提供。
 * 请求中的期望版本用于防止并发修改互相覆盖。</p>
 *
 * @param awardCode       奖品业务编码
 * @param awardName       奖品名称
 * @param awardType       发奖处理器类型编码
 * @param claimMode       领取模式
 * @param providerCode    履约供应商编码；内部奖品可以为空
 * @param awardConfig     奖品发放配置 JSON
 * @param configVersion   配置结构版本
 * @param awardDesc       奖品说明；可以为空
 * @param expectedVersion 调用方最近一次查询到的乐观锁版本
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@Schema(description = "修改草稿奖品定义请求")
public record UpdateAwardDefinitionRequest(
        @Schema(description = "奖品业务编码", example = "credit_200", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "奖品业务编码不能为空")
        @Pattern(regexp = "^[a-z][a-z0-9_-]{1,63}$", message = "奖品业务编码格式不合法")
        String awardCode,

        @Schema(description = "奖品名称", example = "200积分", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "奖品名称不能为空")
        @Size(max = 128, message = "奖品名称长度不能超过 128 个字符")
        String awardName,

        @Schema(description = "发奖类型", example = "internal_credit", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "发奖类型不能为空")
        @Pattern(regexp = "^[a-z][a-z0-9_]{1,31}$", message = "发奖类型格式不合法")
        String awardType,

        @Schema(description = "领取模式", example = "automatic", allowableValues = {"automatic", "manual"})
        @NotBlank(message = "领取模式不能为空")
        @Pattern(regexp = "^(automatic|manual)$", message = "领取模式不合法")
        String claimMode,

        @Schema(description = "履约供应商编码", example = "coupon_provider")
        @Pattern(regexp = "^(?:[a-z][a-z0-9_-]{1,31})?$", message = "履约供应商编码格式不合法")
        String providerCode,

        @Schema(description = "奖品发放配置 JSON", example = "{\"amount\":200}")
        @NotBlank(message = "奖品发放配置不能为空")
        @Size(max = 4000, message = "奖品发放配置长度不能超过 4000 个字符")
        String awardConfig,

        @Schema(description = "配置结构版本", example = "1", minimum = "1")
        @Min(value = 1, message = "配置结构版本不能小于 1")
        int configVersion,

        @Schema(description = "奖品说明", example = "中奖后自动发放二百积分")
        @Size(max = 256, message = "奖品说明长度不能超过 256 个字符")
        String awardDesc,

        @Schema(description = "期望的乐观锁版本", example = "2", minimum = "1")
        @Min(value = 1, message = "期望版本不能小于 1")
        int expectedVersion
) implements Serializable {
}
