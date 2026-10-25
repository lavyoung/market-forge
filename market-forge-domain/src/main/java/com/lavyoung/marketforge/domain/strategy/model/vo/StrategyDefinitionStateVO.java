package com.lavyoung.marketforge.domain.strategy.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * 策略定义生命周期状态。
 *
 * <p>该状态描述运营配置是否允许编辑、是否允许被活动引用，不表示某次抽奖请求
 * 或概率快照的执行状态。抽奖运行时仍然使用已经发布的策略配置和概率快照。</p>
 *
 * <p>策略生命周期固定为：</p>
 *
 * <pre>
 * DRAFT -> ACTIVE -> DISABLED
 * </pre>
 *
 * <p>草稿允许直接修改；已生效和已停用策略都不允许原地修改。需要调整历史策略时，
 * 必须复制为新的草稿策略，避免运行中活动读取到被部分修改的配置。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@Getter
@AllArgsConstructor
public enum StrategyDefinitionStateVO {

    /**
     * 草稿状态，可以编辑基础配置、奖品和规则，但不能被开放活动引用。
     */
    DRAFT("draft", "草稿"),

    /**
     * 已生效状态，可以被活动引用，但不允许原地修改。
     */
    ACTIVE("active", "已生效"),

    /**
     * 已停用状态，不能被新活动引用，也不能重新启用。
     *
     * <p>已经发布的概率快照和历史活动仍然保留，不在停用时破坏性删除。</p>
     */
    DISABLED("disabled", "已停用");

    private final String code;
    private final String description;

    /**
     * 根据数据库编码解析策略生命周期状态。
     *
     * @param code 数据库保存的状态编码
     * @return 匹配的生命周期状态；编码为空或未知时返回空
     */
    public static Optional<StrategyDefinitionStateVO> of(String code) {
        return Arrays.stream(values()).filter(state -> state.code.equals(code)).findFirst();
    }

    /**
     * 判断当前策略是否允许原地编辑。
     *
     * @return 草稿状态返回 {@code true}
     */
    public boolean canEdit() {
        return this == DRAFT;
    }

    /**
     * 判断当前策略是否允许被活动引用。
     *
     * @return 已生效状态返回 {@code true}
     */
    public boolean canReference() {
        return this == ACTIVE;
    }

    /**
     * 判断修改当前策略时是否必须复制为新草稿。
     *
     * @return 已生效或已停用时返回 {@code true}
     */
    public boolean requiresCopyForChange() {
        return this != DRAFT;
    }

    /**
     * 判断当前状态能否迁移到目标状态。
     *
     * @param targetState 目标生命周期状态
     * @return 状态迁移合法时返回 {@code true}
     * @throws NullPointerException 目标状态为空时抛出
     */
    public boolean canTransitionTo(StrategyDefinitionStateVO targetState) {
        Objects.requireNonNull(targetState, "targetState must not be null");
        return switch (this) {
            case DRAFT -> targetState == ACTIVE;
            case ACTIVE -> targetState == DISABLED;
            case DISABLED -> false;
        };
    }
}
