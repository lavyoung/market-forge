package com.lavyoung.marketforge.application.award.model;

/**
 * 奖品领取模式运营元数据。
 *
 * <p>同时提供稳定业务编码和面向运营人员的展示名称，使前端能够直接生成
 * 领取模式选项，不需要维护 {@code automatic}、{@code manual} 的中文映射。</p>
 *
 * <p>本模型属于 Application 输出模型，不直接向 API 暴露领域枚举，
 * 从而避免 Domain 模型成为外部接口契约。</p>
 *
 * @param code        稳定的领取模式编码
 * @param displayName 运营后台展示名称
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
public record AwardClaimModeMetadata(String code, String displayName) {

    /**
     * 创建经过完整性校验的领取模式元数据。
     *
     * @throws IllegalArgumentException 编码或展示名称为空白时抛出
     */
    public AwardClaimModeMetadata {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("claim mode code must not be blank");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("claim mode displayName must not be blank");
        }
    }
}