package com.lavyoung.marketforge.domain.award.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 奖品领取模式。
 *
 * <p>领取模式决定中奖后能否直接创建发奖执行单。自动发放奖品立即进入发奖流程；
 * 人工领取奖品先等待用户补充地址、手机号或其他领取信息。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@Getter
@AllArgsConstructor
public enum AwardClaimModeVO {

    /**
     * 自动发放，例如积分、活动额度和自动到账优惠券。
     */
    AUTOMATIC("automatic", "自动发放"),

    /**
     * 等待用户领取，例如实物奖品或需要用户选择账号的权益。
     */
    MANUAL("manual", "人工领取");

    private final String code;
    private final String desc;


    /**
     * 根据持久化编码解析领取模式。
     *
     * @param code 领取模式编码
     * @return 匹配的领取模式；不存在时返回空
     */
    public static Optional<AwardClaimModeVO> of(String code) {
        return Arrays.stream(values()).filter(value -> value.code.equals(code)).findFirst();
    }
}
