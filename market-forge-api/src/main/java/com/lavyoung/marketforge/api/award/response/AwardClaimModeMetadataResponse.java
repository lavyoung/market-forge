package com.lavyoung.marketforge.api.award.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 奖品领取模式元数据响应。
 *
 * @param code        稳定的领取模式编码
 * @param displayName 运营后台展示名称
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@Schema(description = "奖品领取模式元数据")
public record AwardClaimModeMetadataResponse(
        @Schema(description = "领取模式编码", example = "automatic") String code,
        @Schema(description = "领取模式名称", example = "自动发放") String displayName) {
}
