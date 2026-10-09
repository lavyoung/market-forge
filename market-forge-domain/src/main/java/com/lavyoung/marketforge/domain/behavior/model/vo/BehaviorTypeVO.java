package com.lavyoung.marketforge.domain.behavior.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

/**
 * 用户行为类型。
 *
 * <p>这一层只表达业务含义，不关心数据库和 MQ。
 * 当前第22节先支持签到行为，后续可以扩展支付、分享、邀请等行为。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/08
 */
@AllArgsConstructor
@Getter
public enum BehaviorTypeVO {
    /**
     * 签到行为。
     */
    SIGN("sign", "签到"),

    ;

    private final String code;
    private final String desc;


    public static Optional<BehaviorTypeVO> fromCode(String code) {
        for (BehaviorTypeVO value : BehaviorTypeVO.values()) {
            if (value.code.equals(code)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
