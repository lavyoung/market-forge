package com.lavyoung.marketforge.application.activity.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 活动展台发布任务应用结果。
 *
 * <p>向 Trigger 和管理后台提供稳定的发布任务视图，包括任务状态、当前执行阶段、
 * 尝试次数和失败信息。该模型不暴露领域枚举，也不包含 MyBatis 持久化对象。</p>
 *
 * <p>管理后台可以根据 {@code state} 和 {@code phase} 展示以下信息：</p>
 *
 * <ul>
 *     <li>{@code processing + checking}：正在检查活动配置。</li>
 *     <li>{@code processing + preheating}：正在预热运行数据。</li>
 *     <li>{@code processing + switching}：正在切换生效展台。</li>
 *     <li>{@code succeeded + completed}：展台发布完成。</li>
 *     <li>{@code failed}：发布失败，{@code phase} 表示失败发生的位置。</li>
 * </ul>
 *
 * @param publicationId 发布任务业务标识
 * @param stageId       活动展台业务标识
 * @param stageVersion  本次发布对应的展台版本
 * @param activityId    本次发布的活动标识快照
 * @param channel       投放渠道快照
 * @param source        流量来源快照
 * @param state         发布任务状态编码
 * @param phase         发布执行阶段编码
 * @param attemptCount  已执行的发布尝试次数
 * @param operator      最近一次执行发布的运营人员
 * @param failureCode   最近一次失败的稳定错误码
 * @param failureReason 最近一次失败的安全说明
 * @param publishedAt   发布成功时间
 * @param createTime    发布任务创建时间
 * @param updateTime    最近一次状态变化时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/06
 */
public record ActivityStagePublicationResult(
        Long publicationId,
        Long stageId,
        int stageVersion,
        Long activityId,
        String channel,
        String source,
        String state,
        String phase,
        int attemptCount,
        String operator,
        String failureCode,
        String failureReason,
        LocalDateTime publishedAt,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {

    /**
     * 创建并校验展台发布任务应用结果。
     *
     * @throws NullPointerException     必填字段为空时抛出
     * @throws IllegalArgumentException 版本或尝试次数非法时抛出
     */
    public ActivityStagePublicationResult {
        Objects.requireNonNull(publicationId, "publicationId must not be null");
        Objects.requireNonNull(stageId, "stageId must not be null");
        Objects.requireNonNull(activityId, "activityId must not be null");
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(phase, "phase must not be null");
        Objects.requireNonNull(operator, "operator must not be null");
        Objects.requireNonNull(createTime, "createTime must not be null");
        Objects.requireNonNull(updateTime, "updateTime must not be null");

        if (stageVersion < 1) {
            throw new IllegalArgumentException("stageVersion must be greater than zero");
        }
        if (attemptCount < 1) {
            throw new IllegalArgumentException("attemptCount must be greater than zero");
        }
    }
}
