package com.lavyoung.marketforge.domain.award.model.vo;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 奖品发放类型值对象。
 *
 * <p>奖品类型用于选择对应的 {@code AwardGrantHandler}，只描述“通过什么业务能力发奖”，
 * 不表示具体奖品身份。具体奖品由 {@code awardId} 和 {@code awardCode} 区分。</p>
 *
 * <p>该类型使用开放式字符串值对象而不是枚举，是为了允许后续通过注册新的发奖处理器
 * 扩展内部权益、第三方优惠券或实物履约能力，而不需要修改公共枚举。</p>
 *
 * @param code 奖品类型编码，只允许小写字母、数字和下划线，并且必须以小写字母开头
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
public record AwardTypeVO(String code) {

    /*
     * 必须在奖品类型常量之前初始化。
     *
     * 奖品类型常量的初始化会调用 AwardTypeVO 构造器，而构造器依赖该正则表达式。
     * 如果将它放在常量之后，类初始化期间 CODE_PATTERN 仍为 null。
     */
    private static final Pattern CODE_PATTERN = Pattern.compile("[a-z][a-z0-9_]{1,31}");

    /**
     * 内部积分奖品。
     */
    public static final AwardTypeVO INTERNAL_CREDIT = new AwardTypeVO("internal_credit");

    /**
     * 内部活动参与额度奖品。
     */
    public static final AwardTypeVO INTERNAL_ACTIVITY_SKU = new AwardTypeVO("internal_activity_sku");

    /**
     * 第三方优惠券奖品。
     */
    public static final AwardTypeVO THIRD_PARTY_COUPON = new AwardTypeVO("third_party_coupon");

    /**
     * 需要用户填写领取信息的实物奖品。
     */
    public static final AwardTypeVO PHYSICAL_GOODS = new AwardTypeVO("physical_goods");

    /**
     * 创建并校验奖品类型。
     *
     * @throws NullPointerException     类型编码为空时抛出
     * @throws IllegalArgumentException 类型编码为空白、长度超过 32 或格式不合法时抛出
     */
    public AwardTypeVO {
        Objects.requireNonNull(code, "award type code must not be null");

        if (code.isBlank()) {
            throw new IllegalArgumentException("award type code must not be blank");
        }

        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException(
                    "award type code must start with a lowercase letter and contain only lowercase letters, numbers or underscores"
            );
        }
    }
}