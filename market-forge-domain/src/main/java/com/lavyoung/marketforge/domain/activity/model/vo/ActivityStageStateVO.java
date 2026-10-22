package com.lavyoung.marketforge.domain.activity.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * 活动展台生命周期状态。
 *
 * <p>展台状态负责描述活动是否已经投放到指定渠道和来源，不表示活动本身是否开放。
 * 活动是否可参与仍由 {@link ActivityStateVO} 判断。</p>
 *
 * <p>状态按照 {@code CREATE -> ACTIVE -> EXPIRED} 单向流转。失效配置只保留历史记录，
 * 不允许重新激活，避免旧配置绕过新的发布校验和预热流程。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Getter
@AllArgsConstructor
public enum ActivityStageStateVO {

    /**
     * 待上架，可以调整关联活动，但用户端不可见。
     */
    CREATE("create", "待上架"),

    /**
     * 已生效，用户端可以根据渠道和来源查询到关联活动。
     */
    ACTIVE("active", "已生效"),

    /**
     * 已失效，仅保留历史记录，不允许重新上架。
     */
    EXPIRED("expire", "已失效");

    private final String code;
    private final String desc;

    /**
     * 根据持久化编码解析展台状态。
     *
     * @param code 状态编码
     * @return 匹配的展台状态；编码无法识别时返回空
     */
    public static Optional<ActivityStageStateVO> of(String code) {
        return Arrays.stream(values()).filter(state -> state.code.equals(code)).findFirst();
    }

    /**
     * 判断当前状态是否允许转换为目标状态。
     *
     * @param targetState 目标状态
     * @return 状态转换合法时返回 {@code true}，否则返回 {@code false}
     * @throws NullPointerException 目标状态为空时抛出
     */
    public boolean canTransitionTo(ActivityStageStateVO targetState) {
        Objects.requireNonNull(targetState, "targetState must not be null");
        return switch (this) {
            case CREATE -> targetState == ACTIVE;
            case ACTIVE -> targetState == EXPIRED;
            case EXPIRED -> false;
        };
    }
}
