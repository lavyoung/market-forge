package com.lavyoung.marketforge.domain.award.event;

import java.util.Objects;

/**
 * 中奖发生时固定的奖品履约快照。
 *
 * <p>该快照记录中奖结算时已经生效的奖品履约信息，并随可靠消息一起持久化。
 * 后续即使奖品定义被停用或产生新版本，已经生成的中奖记录仍按照该快照发放，
 * 避免消息重试期间重新读取可变配置而改变发奖结果。</p>
 *
 * <p>本类型是集成事件的一部分，因此使用稳定的字符串编码，而不直接暴露领域枚举。
 * {@code awardCode} 只标识具体奖品；{@code awardType} 才用于选择发奖处理器；
 * {@code providerCode} 用于处理器选择具体履约供应商。</p>
 *
 * @param awardCode     奖品业务编码，仅用于识别具体奖品
 * @param awardType     发奖类型编码，用于选择发奖处理器
 * @param claimMode     领取模式编码，例如 automatic 或 manual
 * @param providerCode  履约供应商编码；平台内部奖品可以为空
 * @param configContent 发奖配置 JSON，由对应类型的处理器解析
 * @param configVersion 发奖配置结构版本，从 1 开始
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/23
 */
public record AwardFulfillmentSnapshot(
        String awardCode,
        String awardType,
        String claimMode,
        String providerCode,
        String configContent,
        int configVersion
) {
    /**
     * 创建并校验奖品履约快照。
     *
     * @throws NullPointerException     必填字段为 {@code null} 时抛出
     * @throws IllegalArgumentException 必填编码为空白、供应商编码为空白或配置版本非法时抛出
     */
    public AwardFulfillmentSnapshot {
        requireText(awardCode, "awardCode");
        requireText(awardType, "awardType");
        requireText(claimMode, "claimMode");
        requireText(configContent, "configContent");

        if (providerCode != null && providerCode.isBlank()) {
            throw new IllegalArgumentException("providerCode must not be blank");
        }
        if (configVersion < 1) {
            throw new IllegalArgumentException("configVersion must be greater than zero");
        }
    }

    private static void requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
