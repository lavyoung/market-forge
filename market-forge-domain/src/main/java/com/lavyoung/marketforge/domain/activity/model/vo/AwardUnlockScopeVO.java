package com.lavyoung.marketforge.domain.activity.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 活动奖品解锁次数统计范围。
 *
 * <p>该值对象决定奖品次数锁应在哪个时间范围内统计用户已经完成的
 * 有效抽奖次数。它只描述统计范围，不包含具体解锁门槛。</p>
 *
 * <p>例如，活动配置为 {@link #DAY}，奖品次数锁门槛配置为 3，
 * 表示用户在当前活动当天完成 3 次抽奖后解锁该奖品。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/17
 */
@Getter
@AllArgsConstructor
public enum AwardUnlockScopeVO {

    /**
     * 按当前活动当天已经完成的抽奖次数计算。
     */
    DAY("day", "活动当日累计次数"),

    /**
     * 按当前活动全部历史已经完成的抽奖次数计算。
     */
    TOTAL("total", "活动历史累计次数");


    private final String code;

    private final String description;

    /**
     * 根据持久化编码查找解锁统计范围。
     *
     * @param code 数据库存储的范围编码
     * @return 匹配的统计范围；编码为空或无法识别时返回空
     */
    public static Optional<AwardUnlockScopeVO> fromCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(scope -> scope.code.equalsIgnoreCase(code.trim()))
                .findFirst();
    }
}
