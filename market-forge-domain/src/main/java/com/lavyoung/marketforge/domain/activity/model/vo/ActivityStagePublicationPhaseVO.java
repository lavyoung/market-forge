package com.lavyoung.marketforge.domain.activity.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * 活动展台发布执行阶段。
 *
 * <p>阶段用于向运营人员展示发布当前执行到哪里，也用于记录失败发生的位置。
 * 发布失败时记录仍保留失败前的阶段，例如在 {@link #PREHEATING} 阶段失败，
 * 页面就可以明确提示“缓存预热失败”。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/05
 */
@Getter
@AllArgsConstructor
public enum ActivityStagePublicationPhaseVO {

    CHECKING("checking", "配置检查"),
    PREHEATING("preheating", "运行数据预热"),
    SWITCHING("switching", "生效展台切换"),
    COMPLETED("completed", "发布完成");

    private final String code;
    private final String description;

    /**
     * 根据持久化编码解析发布阶段。
     *
     * @param code 阶段编码
     * @return 匹配的发布阶段；编码为空或未知时返回空
     */
    public static Optional<ActivityStagePublicationPhaseVO> of(String code) {
        return Arrays.stream(values()).filter(value -> value.code.equals(code)).findFirst();
    }

    /**
     * 判断是否允许从当前阶段推进到目标阶段。
     *
     * @param targetPhase 目标阶段
     * @return 目标阶段是当前阶段的直接后继时返回 {@code true}
     * @throws NullPointerException 目标阶段为空时抛出
     */
    public boolean canAdvanceTo(ActivityStagePublicationPhaseVO targetPhase) {
        Objects.requireNonNull(targetPhase, "targetPhase must not be null");
        return switch (this) {
            case CHECKING -> targetPhase == PREHEATING;
            case PREHEATING -> targetPhase == SWITCHING;
            case SWITCHING -> targetPhase == COMPLETED;
            case COMPLETED -> false;
        };
    }
}
