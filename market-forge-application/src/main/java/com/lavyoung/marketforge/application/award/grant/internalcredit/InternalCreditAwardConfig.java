package com.lavyoung.marketforge.application.award.grant.internalcredit;

import java.util.Objects;

/**
 * 平台内部积分奖品的履约配置。
 *
 * <p>本配置对应 {@code awardType=internal_credit}、{@code configVersion=1}。
 * 数据库存储的 JSON 示例为 {@code {"amount": 100}}。</p>
 *
 * <p>配置模型使用独立 record，而不是让发奖处理器直接解析字符串，
 * 从而为配置校验、版本演进和运营后台表单提供稳定的数据契约。</p>
 *
 * @param amount 本次发放的积分数量，必须大于零
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/23
 */
public record InternalCreditAwardConfig(Long amount) {

    /**
     * 平台内部积分奖品类型。
     */
    public static final String AWARD_TYPE = "internal_credit";

    /**
     * 当前积分配置结构版本。
     */
    public static final int CONFIG_VERSION = 1;

    /**
     * 创建并校验积分奖品配置。
     *
     * @throws NullPointerException     积分数量为空时抛出
     * @throws IllegalArgumentException 积分数量小于等于零时抛出
     */
    public InternalCreditAwardConfig {
        Objects.requireNonNull(amount, "amount must not be null");
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
