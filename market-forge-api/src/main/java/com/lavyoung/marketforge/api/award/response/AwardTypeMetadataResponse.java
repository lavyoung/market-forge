package com.lavyoung.marketforge.api.award.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 系统支持的奖品类型元数据响应。
 *
 * @param awardType           稳定的奖品类型编码
 * @param displayName         奖品类型展示名称
 * @param supportedClaimModes 支持的领取模式
 * @param configVersion       配置结构版本
 * @param configFields        动态表单字段
 * @param description         奖品类型业务说明
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@Schema(description = "可运营配置的奖品类型元数据")
public record AwardTypeMetadataResponse(
        @Schema(description = "奖品类型编码", example = "internal_credit") String awardType,
        @Schema(description = "奖品类型名称", example = "平台积分") String displayName,
        @Schema(description = "支持的领取模式") List<AwardClaimModeMetadataResponse> supportedClaimModes,
        @Schema(description = "配置结构版本", example = "1") int configVersion,
        @Schema(description = "配置表单字段") List<AwardConfigFieldMetadataResponse> configFields,
        @Schema(description = "奖品类型说明", example = "中奖后自动发放到平台积分账户") String description) {

    /**
     * 冻结嵌套集合，保证协议对象创建后不可变。
     *
     * @throws NullPointerException 领取模式或配置字段集合为空时抛出
     */
    public AwardTypeMetadataResponse {
        supportedClaimModes = List.copyOf(supportedClaimModes);
        configFields = List.copyOf(configFields);
    }
}
