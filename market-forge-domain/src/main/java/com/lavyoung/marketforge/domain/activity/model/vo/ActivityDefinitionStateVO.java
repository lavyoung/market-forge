package com.lavyoung.marketforge.domain.activity.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * 活动定义生命周期状态。
 *
 * <p>草稿允许编辑，开放状态允许参与和上架，关闭状态为终态。已开放活动不允许原地
 * 修改核心配置，避免已经产生的订单与后续抽奖使用不同规则。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Getter
@AllArgsConstructor
public enum ActivityDefinitionStateVO {
    DRAFT("draft", "草稿"),
    OPEN("open", "开放"),
    CLOSE("close", "关闭");

    private final String code;
    private final String description;

    /**
     * 根据持久化编码解析生命周期状态。
     *
     * @param code 状态编码
     * @return 匹配的状态；编码为空或未知时返回空
     */
    public static Optional<ActivityDefinitionStateVO> of(String code) {
        return Arrays.stream(values()).filter(value -> value.code.equals(code)).findFirst();
    }

    /**
     * 判断活动定义是否允许编辑。
     *
     * @return 草稿状态返回 {@code true}
     */
    public boolean canEdit() {
        return this == DRAFT;
    }

    /**
     * 判断是否允许迁移到目标状态。
     *
     * @param targetState 目标状态
     * @return 状态迁移合法时返回 {@code true}
     * @throws NullPointerException 目标状态为空时抛出
     */
    public boolean canTransitionTo(ActivityDefinitionStateVO targetState) {
        Objects.requireNonNull(targetState, "targetState must not be null");
        return switch (this) {
            case DRAFT -> targetState == OPEN;
            case OPEN -> targetState == CLOSE;
            case CLOSE -> false;
        };
    }
}
