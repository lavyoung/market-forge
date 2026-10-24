package com.lavyoung.marketforge.application.award.model;

import java.util.List;
import java.util.Objects;

/**
 * 一种可用奖品类型的完整元数据。
 *
 * <p>该模型描述系统当前真实支持的奖品类型、领取模式、配置版本和配置字段。
 * 运营后台只能使用该列表中的类型创建奖品，避免配置无法履约的奖品。</p>
 *
 * @param awardType           稳定的奖品类型编码，用于选择发奖处理器
 * @param displayName         运营后台展示名称
 * @param supportedClaimModes 支持的领取模式元数据
 * @param configVersion       当前配置结构版本
 * @param configFields        配置表单字段，可以为空集合
 * @param description         奖品类型的业务说明
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/24
 */
public record AwardTypeMetadata(
        String awardType,
        String displayName,
        List<AwardClaimModeMetadata> supportedClaimModes,
        int configVersion,
        List<AwardConfigFieldMetadata> configFields,
        String description
) {
    /**
     * 创建经过完整性校验的奖品类型元数据。
     *
     * @throws NullPointerException     领取模式或配置字段集合为空时抛出
     * @throws IllegalArgumentException 类型、名称、说明为空白，版本非法或领取模式重复时抛出
     */
    public AwardTypeMetadata {
        if (awardType == null || awardType.isBlank()) {
            throw new IllegalArgumentException("awardType must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName must not be blank");
        }
        if (configVersion <= 0) {
            throw new IllegalArgumentException("configVersion must be positive");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }

        Objects.requireNonNull(supportedClaimModes, "supportedClaimModes must not be null");
        Objects.requireNonNull(configFields, "configFields must not be null");
        supportedClaimModes = List.copyOf(supportedClaimModes);
        configFields = List.copyOf(configFields);

        if (supportedClaimModes.isEmpty()) {
            throw new IllegalArgumentException("supportedClaimModes must not be empty");
        }
        if (supportedClaimModes.stream().map(AwardClaimModeMetadata::code).distinct().count()
                != supportedClaimModes.size()) {
            throw new IllegalArgumentException("supportedClaimModes must not contain duplicate codes");
        }
    }
}
