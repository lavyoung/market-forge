package com.lavyoung.marketforge.domain.award.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

/**
 * 用户中奖奖品状态。
 * <p>
 * 描述用户中奖记录当前需要承载的发奖状态。枚举编码会持久化到
 * {@code user_award_record.award_state} 字段，需与表结构默认值和当前发奖流程保持一致。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/23
 */
@Getter
@AllArgsConstructor
public enum AwardStateVO {

    /**
     * 中奖记录已创建，尚未进入领取或发奖流程。
     */
    CREATE("create", "已创建"),

    /**
     * 等待用户补充领取信息。
     */
    WAIT_CLAIM("wait_claim", "待领取"),

    /**
     * 奖品发放处理中。
     */
    GRANTING("granting", "发奖中"),

    /**
     * 奖品发放失败，等待重试或人工补偿。
     */
    GRANT_FAILED("grant_failed", "发奖失败"),

    /**
     * 奖品已发放完成。
     */
    COMPLETED("completed", "发奖完成"),

    ;

    /**
     * 状态编码，持久化到用户中奖记录表。
     */
    private final String code;

    /**
     * 状态说明，用于日志和后台展示。
     */
    private final String desc;

    /**
     * 根据状态编码解析奖品状态。
     *
     * @param code 状态编码
     * @return 匹配的奖品状态；不存在时返回空
     */
    public static Optional<AwardStateVO> of(String code) {
        return Arrays.stream(values())
                .filter(value -> value.code.equals(code))
                .findFirst();
    }

    /**
     * 判断当前状态是否为最终状态。
     * <p>
     * 最终状态表示后续不会再自动推进，通常只能通过人工补偿或重新创建记录处理。
     *
     * @return 当前状态为发奖完成时返回 {@code true}
     */
    public boolean isTerminal() {
        return this == COMPLETED;
    }

    /**
     * 判断当前状态是否允许用户提交领取信息。
     *
     * @return 当前状态为已创建或待领取时返回 {@code true}
     */
    public boolean canClaim() {
        return this == CREATE || this == WAIT_CLAIM;
    }

    /**
     * 判断当前状态是否允许进入发奖流程。
     *
     * @return 当前状态为已创建或待领取时返回 {@code true}
     */
    public boolean canGrant() {
        return this == CREATE || this == WAIT_CLAIM;
    }
}
