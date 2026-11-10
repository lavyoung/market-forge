package com.lavyoung.marketforge.api.activity.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 活动展台发布任务响应。
 *
 * <p>用于向运营后台展示发布任务的整体状态、当前执行阶段、重试次数和安全失败信息。
 * 发布任务、展台和活动标识均使用字符串，避免十八位业务编号在浏览器中发生精度丢失。</p>
 *
 * @param publicationId 发布任务业务标识
 * @param stageId       活动展台业务标识
 * @param stageVersion  本次发布对应的展台版本
 * @param activityId    本次发布的活动标识快照
 * @param channel       投放渠道快照
 * @param source        流量来源快照
 * @param state         发布任务状态
 * @param phase         当前执行阶段或失败所在阶段
 * @param attemptCount  已执行的发布尝试次数
 * @param operator      最近一次执行发布的运营人员
 * @param failureCode   最近一次失败的稳定错误编码
 * @param failureReason 最近一次失败的安全说明
 * @param publishedAt   发布成功时间
 * @param createTime    发布任务创建时间
 * @param updateTime    最近一次状态变化时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/10
 */
@Schema(description = "活动展台发布任务")
public record ActivityStagePublicationResponse(
        @Schema(description = "发布任务业务标识", example = "9007199254740993", type = "string") String publicationId,
        @Schema(description = "活动展台业务标识", example = "9007199254740994", type = "string") String stageId,
        @Schema(description = "本次发布对应的展台版本", example = "2") int stageVersion,
        @Schema(description = "活动业务标识快照", example = "9007199254740995", type = "string") String activityId,
        @Schema(description = "投放渠道", example = "web") String channel,
        @Schema(description = "流量来源", example = "official") String source,
        @Schema(description = "发布状态", example = "processing", allowableValues = {"processing", "succeeded", "failed"}) String state,
        @Schema(description = "执行阶段", example = "preheating", allowableValues = {"checking", "preheating", "switching", "completed"}) String phase,
        @Schema(description = "发布尝试次数", example = "1") int attemptCount,
        @Schema(description = "最近一次执行发布的运营人员", example = "admin") String operator,
        @Schema(description = "失败错误编码", example = "activity.assembly.failed", nullable = true) String failureCode,
        @Schema(description = "失败安全说明", example = "活动装配失败", nullable = true) String failureReason,
        @Schema(description = "发布成功时间", nullable = true) LocalDateTime publishedAt,
        @Schema(description = "发布任务创建时间") LocalDateTime createTime,
        @Schema(description = "最近一次状态变化时间") LocalDateTime updateTime
) implements Serializable {
}
