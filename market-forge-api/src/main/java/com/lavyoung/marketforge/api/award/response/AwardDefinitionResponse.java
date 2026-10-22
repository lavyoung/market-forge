package com.lavyoung.marketforge.api.award.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 奖品定义响应。
 *
 * <p>用于运营后台展示奖品配置、生命周期和审计信息。
 * 发奖配置原样返回，以便草稿编辑页面回填。</p>
 *
 * @param awardId       奖品业务标识，以字符串返回以避免 JavaScript 整数精度丢失
 * @param awardCode     奖品业务编码
 * @param awardName     奖品名称
 * @param awardType     发奖处理器类型编码
 * @param claimMode     领取模式
 * @param providerCode  履约供应商编码
 * @param awardConfig   奖品发放配置 JSON
 * @param configVersion 配置结构版本
 * @param awardDesc     奖品说明
 * @param state         生命周期状态
 * @param version       乐观锁版本
 * @param createdBy     创建人
 * @param updatedBy     最后修改人
 * @param createTime    创建时间
 * @param updateTime    最后修改时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Schema(description = "奖品定义")
public record AwardDefinitionResponse(
        @Schema(description = "奖品业务标识，以字符串返回以避免 JavaScript 整数精度丢失", example = "100101", type = "string")
        String awardId,

        @Schema(description = "奖品业务编码", example = "credit_100")
        String awardCode,

        @Schema(description = "奖品名称", example = "100积分")
        String awardName,

        @Schema(description = "发奖类型", example = "internal_credit")
        String awardType,

        @Schema(description = "领取模式", example = "automatic", allowableValues = {"automatic", "manual"})
        String claimMode,

        @Schema(description = "履约供应商编码", example = "coupon_provider")
        String providerCode,

        @Schema(description = "奖品发放配置 JSON", example = "{\"amount\":100}")
        String awardConfig,

        @Schema(description = "配置结构版本", example = "1")
        int configVersion,

        @Schema(description = "奖品说明", example = "中奖后自动发放到用户积分账户")
        String awardDesc,

        @Schema(description = "生命周期状态", example = "draft",
                allowableValues = {"draft", "active", "disabled"})
        String state,

        @Schema(description = "乐观锁版本", example = "1")
        int version,

        @Schema(description = "创建人", example = "admin")
        String createdBy,

        @Schema(description = "最后修改人", example = "admin")
        String updatedBy,

        @Schema(description = "创建时间", example = "2026-10-22T10:00:00",
                type = "string", format = "date-time")
        LocalDateTime createTime,

        @Schema(description = "最后修改时间", example = "2026-10-22T10:30:00",
                type = "string", format = "date-time")
        LocalDateTime updateTime
) implements Serializable {
}
