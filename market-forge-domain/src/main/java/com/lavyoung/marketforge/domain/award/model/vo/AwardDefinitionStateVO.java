package com.lavyoung.marketforge.domain.award.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * 奖品定义生命周期状态。
 *
 * <p>状态只描述奖品配置是否允许编辑或被新策略引用，不表示某一用户奖品的发放状态。
 * 用户中奖后的状态继续由 {@link AwardStateVO} 表达。</p>
 *
 * <p>奖品启用后不允许重新编辑；停用状态为终态。如果需要调整已启用奖品的履约配置，
 * 应创建新的奖品定义版本，避免历史中奖记录的履约语义发生变化。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/21
 */
@Getter
@AllArgsConstructor
public enum AwardDefinitionStateVO {

    /**
     * 草稿配置，可以编辑，但不能加入可发布策略。
     */
    DRAFT("draft", "草稿"),

    /**
     * 已启用配置，可以被策略引用，核心发奖参数不允许原地修改。
     */
    ACTIVE("active", "已启用"),

    /**
     * 已停用配置，已有中奖快照仍可继续履约，但不能被新策略引用。
     */
    DISABLED("disabled", "已停用");

    private final String code;
    private final String desc;

    /**
     * 根据持久化编码解析奖品定义状态。
     *
     * @param code 奖品定义状态编码
     * @return 匹配的状态；不存在时返回空
     */
    public static Optional<AwardDefinitionStateVO> of(String code) {
        return Arrays.stream(values()).filter(value -> value.code.equals(code)).findFirst();
    }

    /**
     * 判断当前奖品定义是否允许直接编辑。
     *
     * @return 处于草稿状态时返回 {@code true}
     */
    public boolean canEdit() {
        return this == DRAFT;
    }

    /**
     * 判断当前奖品定义是否允许被新策略引用。
     *
     * @return 处于启用状态时返回 {@code true}
     */
    public boolean canReference() {
        return this == ACTIVE;
    }

    /**
     * 判断当前状态是否允许转换到目标状态。
     *
     * <p>当前支持的生命周期如下：</p>
     *
     * <pre>
     * DRAFT -> ACTIVE -> DISABLED
     * </pre>
     *
     * <p>停用状态是终态，不能重新启用。若已启用奖品的配置需要变化，
     * 应复制并创建新的草稿奖品定义。</p>
     *
     * @param targetState 目标状态
     * @return 允许转换返回 {@code true}，否则返回 {@code false}
     * @throws NullPointerException 目标状态为空时抛出
     */
    public boolean canTransitionTo(AwardDefinitionStateVO targetState) {
        Objects.requireNonNull(targetState, "targetState must not be null");
        return switch (this) {
            case DRAFT -> targetState == ACTIVE;
            case ACTIVE -> targetState == DISABLED;
            case DISABLED -> false;
        };
    }
}