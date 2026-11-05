package com.lavyoung.marketforge.domain.activity.model.entity;

import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStagePublicationPhaseVO;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStagePublicationStateVO;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 活动展台发布记录。
 *
 * <p>一条记录对应某个展台版本的发布任务。通过“展台标识 + 展台版本”建立
 * 唯一约束，可以保证同一个配置版本只有一个发布任务。失败后复用原记录重试，
 * 而不是重复创建发布记录。</p>
 *
 * <p>记录保存活动、渠道和来源快照，即使展台后续被修改，也可以准确还原本次
 * 发布针对的配置。失败信息只能保存稳定错误码和安全错误说明，禁止写入异常
 * 堆栈、数据库连接信息或其他敏感内容。</p>
 *
 * @param publicationId 发布记录业务标识
 * @param stageId       展台业务标识
 * @param stageVersion  本次发布对应的展台版本
 * @param activityId    本次发布的活动标识快照
 * @param channel       投放渠道快照
 * @param source        流量来源快照
 * @param state         发布任务状态
 * @param phase         当前阶段或失败时所在阶段
 * @param attemptCount  已执行的发布尝试次数
 * @param operator      最近一次执行发布的运营人员
 * @param failureCode   最近一次失败的稳定错误码；未失败时为空
 * @param failureReason 最近一次失败的安全说明；未失败时为空
 * @param publishedAt   成功发布时间；尚未成功时为空
 * @param createTime    发布记录创建时间
 * @param updateTime    最近一次状态变化时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/05
 */
public record ActivityStagePublicationEntity(
        Long publicationId,
        Long stageId,
        int stageVersion,
        Long activityId,
        String channel,
        String source,
        ActivityStagePublicationStateVO state,
        ActivityStagePublicationPhaseVO phase,
        int attemptCount,
        String operator,
        String failureCode,
        String failureReason,
        LocalDateTime publishedAt,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
    /**
     * 创建并校验展台发布记录。
     *
     * @throws NullPointerException     必填字段为空时抛出
     * @throws IllegalArgumentException 标识、版本、尝试次数或状态组合非法时抛出
     */
    public ActivityStagePublicationEntity {
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

        if (publicationId <= 0 || stageId <= 0 || activityId <= 0) {
            throw new IllegalArgumentException("business identifiers must be positive");
        }
        if (stageVersion < 1) {
            throw new IllegalArgumentException("stageVersion must be greater than zero");
        }
        if (attemptCount < 1) {
            throw new IllegalArgumentException("attemptCount must be greater than zero");
        }
        if (operator.isBlank() || operator.length() > 64) {
            throw new IllegalArgumentException("operator must contain 1 to 64 characters");
        }
        if (state != ActivityStagePublicationStateVO.SUCCEEDED
                && phase == ActivityStagePublicationPhaseVO.COMPLETED) {
            throw new IllegalArgumentException("only succeeded publication can be completed");
        }
        if (state == ActivityStagePublicationStateVO.SUCCEEDED
                && (phase != ActivityStagePublicationPhaseVO.COMPLETED || publishedAt == null)) {
            throw new IllegalArgumentException("succeeded publication must be completed and published");
        }
        if (state != ActivityStagePublicationStateVO.SUCCEEDED && publishedAt != null) {
            throw new IllegalArgumentException("unfinished publication cannot have publishedAt");
        }
        if (state == ActivityStagePublicationStateVO.FAILED
                && (failureCode == null || failureCode.isBlank()
                || failureReason == null || failureReason.isBlank())) {
            throw new IllegalArgumentException("failed publication must contain failure information");
        }
        if (state != ActivityStagePublicationStateVO.FAILED
                && (failureCode != null || failureReason != null)) {
            throw new IllegalArgumentException("non-failed publication cannot contain failure information");
        }
    }

    /**
     * 根据待发布展台创建首次发布记录。
     *
     * @param publicationId 新发布记录业务标识
     * @param stage         待发布展台快照
     * @param operator      当前运营人员
     * @param startedAt     首次发布开始时间
     * @return 处于配置检查阶段的发布记录
     * @throws NullPointerException     参数为空时抛出
     * @throws IllegalArgumentException 展台不是待上架状态或字段不合法时抛出
     */
    public static ActivityStagePublicationEntity start(Long publicationId, ActivityStageEntity stage,
                                                       String operator, LocalDateTime startedAt) {
        Objects.requireNonNull(stage, "stage must not be null");
        Objects.requireNonNull(startedAt, "startedAt must not be null");
        if (!stage.canEdit()) {
            throw new IllegalArgumentException("only draft stage can start publication");
        }
        return new ActivityStagePublicationEntity(
                publicationId,
                stage.stageId(),
                stage.version(),
                stage.activityId(),
                stage.channel(),
                stage.source(),
                ActivityStagePublicationStateVO.PROCESSING,
                ActivityStagePublicationPhaseVO.CHECKING,
                1,
                operator,
                null,
                null,
                null,
                startedAt,
                startedAt
        );
    }

    /**
     * 推进到下一个发布阶段。
     *
     * @param targetPhase 目标发布阶段
     * @param operator    当前运营人员
     * @param changedAt   阶段变化时间
     * @return 推进阶段后的新发布记录
     * @throws IllegalStateException 当前任务不在处理中或阶段不能直接推进时抛出
     */
    public ActivityStagePublicationEntity advance(ActivityStagePublicationPhaseVO targetPhase, String operator, LocalDateTime changedAt) {
        if (state != ActivityStagePublicationStateVO.PROCESSING || targetPhase == ActivityStagePublicationPhaseVO.COMPLETED
                || !phase.canAdvanceTo(targetPhase)) {
            throw new IllegalStateException("publication phase transition is not allowed");
        }
        return copy(
                ActivityStagePublicationStateVO.PROCESSING,
                targetPhase,
                attemptCount,
                operator,
                null,
                null,
                null,
                changedAt
        );
    }

    /**
     * 将当前发布任务标记为失败。
     *
     * <p>阶段保持不变，用于记录失败发生在检查、预热还是切换阶段。</p>
     *
     * @param failureCode   稳定的失败编码
     * @param failureReason 面向运营人员的安全失败说明
     * @param operator      当前运营人员
     * @param failedAt      失败时间
     * @return 失败状态的新发布记录
     * @throws IllegalStateException 当前任务不在处理中时抛出
     */
    public ActivityStagePublicationEntity fail(String failureCode, String failureReason, String operator, LocalDateTime failedAt) {
        if (!state.canTransitionTo(ActivityStagePublicationStateVO.FAILED)) {
            throw new IllegalStateException("publication cannot be marked as failed");
        }
        return copy(
                ActivityStagePublicationStateVO.FAILED,
                phase,
                attemptCount,
                operator,
                failureCode,
                failureReason,
                null,
                failedAt
        );
    }

    /**
     * 重新开始失败的发布任务。
     *
     * <p>重试复用原发布记录并增加尝试次数，同时清除上次失败信息，
     * 不要求运营人员重新创建展台。</p>
     *
     * @param operator  当前运营人员
     * @param retriedAt 重试开始时间
     * @return 重新进入配置检查阶段的新发布记录
     * @throws IllegalStateException 当前任务不是失败状态时抛出
     */
    public ActivityStagePublicationEntity retry(String operator, LocalDateTime retriedAt) {
        if (!state.canTransitionTo(ActivityStagePublicationStateVO.PROCESSING)) {
            throw new IllegalStateException("only failed publication can be retried");
        }

        return copy(
                ActivityStagePublicationStateVO.PROCESSING,
                ActivityStagePublicationPhaseVO.CHECKING,
                attemptCount + 1,
                operator,
                null,
                null,
                null,
                retriedAt
        );
    }

    /**
     * 将完成展台切换的发布任务标记为成功。
     *
     * @param operator    当前运营人员
     * @param publishedAt 发布完成时间
     * @return 成功终态的新发布记录
     * @throws IllegalStateException 当前任务不在切换阶段时抛出
     */
    public ActivityStagePublicationEntity succeed(String operator, LocalDateTime publishedAt) {
        if (state != ActivityStagePublicationStateVO.PROCESSING
                || phase != ActivityStagePublicationPhaseVO.SWITCHING
                || !state.canTransitionTo(ActivityStagePublicationStateVO.SUCCEEDED)) {
            throw new IllegalStateException("only switching publication can succeed");
        }

        return copy(
                ActivityStagePublicationStateVO.SUCCEEDED,
                ActivityStagePublicationPhaseVO.COMPLETED,
                attemptCount,
                operator,
                null,
                null,
                publishedAt,
                publishedAt
        );
    }

    private ActivityStagePublicationEntity copy(
            ActivityStagePublicationStateVO targetState,
            ActivityStagePublicationPhaseVO targetPhase,
            int targetAttemptCount,
            String targetOperator,
            String targetFailureCode,
            String targetFailureReason,
            LocalDateTime targetPublishedAt,
            LocalDateTime targetUpdateTime
    ) {
        return new ActivityStagePublicationEntity(
                publicationId,
                stageId,
                stageVersion,
                activityId,
                channel,
                source,
                targetState,
                targetPhase,
                targetAttemptCount,
                targetOperator,
                targetFailureCode,
                targetFailureReason,
                targetPublishedAt,
                createTime,
                targetUpdateTime
        );
    }
}
