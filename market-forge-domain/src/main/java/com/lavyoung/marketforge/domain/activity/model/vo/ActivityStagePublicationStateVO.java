package com.lavyoung.marketforge.domain.activity.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * 活动展台发布任务状态。
 *
 * <p>状态描述一条展台发布记录的整体执行结果。发布过程中的具体步骤由
 * {@link ActivityStagePublicationPhaseVO} 表达，避免把“正在预热”等执行阶段
 * 与“执行成功、执行失败”等任务结果混为一谈。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/05
 */
@Getter
@AllArgsConstructor
public enum ActivityStagePublicationStateVO {

    PROCESSING("processing", "处理中"),
    SUCCEEDED("succeeded", "发布成功"),
    FAILED("failed", "发布失败");

    private final String code;
    private final String description;

    /**
     * 根据持久化编码解析发布状态。
     *
     * @param code 状态编码
     * @return 匹配的发布状态；编码为空或未知时返回空
     */
    public static Optional<ActivityStagePublicationStateVO> of(String code) {
        return Arrays.stream(values()).filter(value -> value.code.equals(code)).findFirst();
    }

    /**
     * 判断当前状态是否允许迁移到目标状态。
     *
     * <p>处理中任务可以成功或失败；失败任务可以重新进入处理状态；
     * 成功任务是终态，重复请求应直接返回已有结果。</p>
     *
     * @param targetState 目标状态
     * @return 允许迁移时返回 {@code true}
     * @throws NullPointerException 目标状态为空时抛出
     */
    public boolean canTransitionTo(ActivityStagePublicationStateVO targetState) {
        Objects.requireNonNull(targetState, "targetState must not be null");
        return switch (this) {
            case PROCESSING -> targetState == SUCCEEDED || targetState == FAILED;
            case FAILED -> targetState == PROCESSING;
            case SUCCEEDED -> false;
        };
    }
}
