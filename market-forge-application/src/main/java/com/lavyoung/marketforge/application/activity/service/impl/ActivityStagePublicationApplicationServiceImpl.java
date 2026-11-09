package com.lavyoung.marketforge.application.activity.service.impl;

import com.lavyoung.marketforge.application.activity.model.ActivityReadinessResult;
import com.lavyoung.marketforge.application.activity.model.ActivityStagePublicationResult;
import com.lavyoung.marketforge.application.activity.model.PublishActivityStageCommand;
import com.lavyoung.marketforge.application.activity.service.IActivityRaffleApplicationService;
import com.lavyoung.marketforge.application.activity.service.IActivityReadinessApplicationService;
import com.lavyoung.marketforge.application.activity.service.IActivityStagePublicationApplicationService;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStageEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStagePublicationEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStagePublicationPhaseVO;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStateVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.repository.IActivityStagePublicationRepository;
import com.lavyoung.marketforge.domain.activity.repository.IActivityStageRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.model.CommonResponseCode;
import com.lavyoung.marketforge.types.utils.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * 活动展台发布应用服务默认实现。
 *
 * <p>负责协调发布任务的幂等创建、失败重试、配置检查、运行数据预热、展台切换和失败留痕。
 * 领域实体判断状态迁移是否合法，仓储执行唯一键和条件更新控制，应用服务只负责编排完整用例。</p>
 *
 * <p>同一展台版本的并发发布请求中，只有成功创建发布任务或成功将失败任务推进为重试状态的
 * 请求可以执行副作用。其他请求直接返回数据库中的最新任务进度，避免重复预热和重复切换展台。</p>
 *
 * <p>配置检查和运行数据预热位于数据库事务外。展台切换与发布成功记录由
 * {@link ActivityStagePublicationTransaction} 在同一本地事务中提交，避免长事务和状态不一致。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/09
 */
@Service
@RequiredArgsConstructor
public class ActivityStagePublicationApplicationServiceImpl implements IActivityStagePublicationApplicationService {

    private static final int PUBLICATION_ID_LENGTH = 18;

    private final IActivityStageRepository activityStageRepository;
    private final IActivityStagePublicationRepository activityStagePublicationRepository;
    private final IActivityRepository activityRepository;
    private final IActivityReadinessApplicationService activityReadinessApplicationService;
    private final IActivityRaffleApplicationService activityRaffleApplicationService;
    private final ActivityStagePublicationTransaction publicationTransaction;
    private final Clock clock;

    @Override
    public ActivityStagePublicationResult publish(PublishActivityStageCommand command) {
        PublishActivityStageCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        String operator = requireOperator(validCommand.operator());
        ActivityStageEntity stage = queryStageRequired(validCommand.stageId());

        PublicationAttempt attempt = prepareAttempt(stage, validCommand.expectedVersion(), operator);
        if (!attempt.executable()) {
            return toResult(attempt.publication());
        }

        return toResult(executePublication(stage, attempt.publication(), operator));
    }

    @Override
    public ActivityStagePublicationResult query(Long stageId, int stageVersion) {
        return toResult(queryPublicationRequired(stageId, stageVersion));
    }

    /**
     * 准备发布任务并判断当前请求是否取得执行权。
     *
     * <p>必须先查询已有发布任务，再校验展台草稿状态。展台发布成功后会变成生效状态并递增版本，
     * 但相同发布请求再次到达时仍应返回成功结果，不能因为草稿校验失败而破坏接口幂等性。</p>
     *
     * @param stage           当前展台快照
     * @param expectedVersion 调用方读取到的展台版本
     * @param operator        当前运营人员
     * @return 发布任务和当前请求的执行权
     * @throws BusinessException 展台状态或版本不允许发布时抛出
     */
    private PublicationAttempt prepareAttempt(ActivityStageEntity stage, int expectedVersion, String operator) {
        Optional<ActivityStagePublicationEntity> existing =
                activityStagePublicationRepository.queryByStageAndVersion(stage.stageId(), expectedVersion);
        if (existing.isPresent()) {
            return prepareExistingAttempt(stage, existing.get(), operator);
        }

        ensureDraftAndVersion(stage, expectedVersion);
        LocalDateTime startedAt = LocalDateTime.now(clock);
        Long publicationId = Long.valueOf(IdGenerator.nextNumericId(PUBLICATION_ID_LENGTH));
        ActivityStagePublicationEntity started = ActivityStagePublicationEntity.start(publicationId, stage, operator, startedAt);
        if (activityStagePublicationRepository.createIfAbsent(started)) {
            return new PublicationAttempt(started, true);
        }

        ActivityStagePublicationEntity concurrent = queryPublicationRequired(stage.stageId(), expectedVersion);
        return prepareExistingAttempt(stage, concurrent, operator);
    }

    /**
     * 处理已经存在的发布任务。
     *
     * <p>成功任务和处理中任务只返回进度；失败任务通过条件更新竞争重试执行权。</p>
     *
     * @param stage       当前展台快照
     * @param publication 已存在的发布任务
     * @param operator    当前运营人员
     * @return 发布任务和当前请求的执行权
     * @throws BusinessException 失败任务对应的展台已经变化时抛出
     */
    private PublicationAttempt prepareExistingAttempt(ActivityStageEntity stage, ActivityStagePublicationEntity publication,
                                                      String operator) {
        return switch (publication.state()) {
            case SUCCEEDED, PROCESSING -> new PublicationAttempt(publication, false);
            case FAILED -> retryFailedPublication(stage, publication, operator);
        };
    }

    /**
     * 尝试重新执行失败的发布任务。
     *
     * <p>重试前重新校验展台快照。若条件更新失败，说明其他请求已经取得执行权，当前请求只返回
     * 最新任务进度，不重复执行发布副作用。</p>
     *
     * @param stage       当前展台快照
     * @param publication 失败发布任务
     * @param operator    当前运营人员
     * @return 重试后的发布任务和执行权
     * @throws BusinessException 展台状态或版本已经变化时抛出
     */
    private PublicationAttempt retryFailedPublication(ActivityStageEntity stage, ActivityStagePublicationEntity publication,
                                                      String operator) {
        ensureDraftAndVersion(stage, publication.stageVersion());
        ActivityStagePublicationEntity retried = publication.retry(operator, LocalDateTime.now(clock));
        if (activityStagePublicationRepository.transition(publication, retried)) {
            return new PublicationAttempt(retried, true);
        }

        ActivityStagePublicationEntity latest = queryPublicationRequired(publication.stageId(), publication.stageVersion());
        return new PublicationAttempt(latest, false);
    }

    /**
     * 执行已经取得所有权的完整发布流程。
     *
     * <ol>
     *     <li>检查活动是否开放以及全部运行配置是否就绪。</li>
     *     <li>推进到预热阶段并装配活动运行数据。</li>
     *     <li>推进到切换阶段，原子切换展台并完成发布任务。</li>
     *     <li>任一步骤失败时，在当前阶段记录稳定失败编码和安全文案。</li>
     * </ol>
     *
     * @param stage       待发布展台快照
     * @param publication 当前发布任务
     * @param operator    当前运营人员
     * @return 发布成功终态
     * @throws RuntimeException 检查、预热、阶段推进或事务切换失败时抛出
     */
    private ActivityStagePublicationEntity executePublication(ActivityStageEntity stage,
                                                              ActivityStagePublicationEntity publication,
                                                              String operator) {
        ActivityStagePublicationEntity current = publication;
        try {
            ensureActivityCanPublish(stage.activityId());
            ensureActivityReadyToPublish(stage.activityId());

            current = advance(current, ActivityStagePublicationPhaseVO.PREHEATING, operator);
            activityRaffleApplicationService.armory(stage.activityId());

            current = advance(current, ActivityStagePublicationPhaseVO.SWITCHING, operator);
            ActivityStagePublicationEntity succeeded = current.succeed(operator, LocalDateTime.now(clock));
            publicationTransaction.publish(stage, current, succeeded);
            return succeeded;
        } catch (RuntimeException exception) {
            recordFailure(current, operator, exception);
            throw exception;
        }
    }

    /**
     * 使用当前任务快照条件推进发布阶段。
     *
     * @param current     当前发布任务
     * @param targetPhase 直接后继阶段
     * @param operator    当前运营人员
     * @return 已持久化的新阶段任务
     * @throws BusinessException 当前快照已被其他请求推进时抛出
     */
    private ActivityStagePublicationEntity advance(ActivityStagePublicationEntity current,
                                                   ActivityStagePublicationPhaseVO targetPhase,
                                                   String operator) {
        ActivityStagePublicationEntity target = current.advance(targetPhase, operator, LocalDateTime.now(clock));
        if (!activityStagePublicationRepository.transition(current, target)) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_STAGE_PUBLICATION_CONFLICT,
                    current.publicationId(), current.state().getCode(), current.phase().getCode(), current.attemptCount());
        }
        return target;
    }

    /**
     * 记录发布失败，同时保留导致发布失败的原始异常。
     *
     * <p>数据库只保存稳定错误编码和安全默认文案。失败状态写入异常会追加到原异常，
     * 不会覆盖真正导致发布失败的原因。发布记录并发冲突不标记失败，因为其他请求可能已经推进任务。</p>
     *
     * @param current   失败发生时的任务快照
     * @param operator  当前运营人员
     * @param exception 原始发布异常
     */
    private void recordFailure(ActivityStagePublicationEntity current, String operator, RuntimeException exception) {
        if (isPublicationConflict(exception)) {
            return;
        }

        PublicationFailure failure = describeFailure(exception);
        try {
            ActivityStagePublicationEntity failed = current.fail(
                    failure.code(), failure.reason(), operator, LocalDateTime.now(clock));
            if (!activityStagePublicationRepository.transition(current, failed)) {
                exception.addSuppressed(BusinessException.of(BusinessResponseCode.ACTIVITY_STAGE_PUBLICATION_CONFLICT,
                        current.publicationId(), current.state().getCode(), current.phase().getCode(),
                        current.attemptCount()));
            }
        } catch (RuntimeException recordingException) {
            exception.addSuppressed(recordingException);
        }
    }

    /**
     * 将异常转换为允许安全持久化的失败信息。
     *
     * @param exception 发布流程异常
     * @return 稳定错误编码和安全错误说明
     */
    private PublicationFailure describeFailure(RuntimeException exception) {
        if (exception instanceof BusinessException businessException) {
            return new PublicationFailure(businessException.getI18nKey(), businessException.getResponseCode().getMsg());
        }
        return new PublicationFailure(CommonResponseCode.SYSTEM_ERROR.getI18nKey(), CommonResponseCode.SYSTEM_ERROR.getMsg());
    }

    /**
     * 判断异常是否表示发布记录发生并发冲突。
     *
     * @param exception 待判断异常
     * @return 是发布记录并发冲突时返回 {@code true}
     */
    private boolean isPublicationConflict(RuntimeException exception) {
        return exception instanceof BusinessException businessException
                && businessException.getCode() == BusinessResponseCode.ACTIVITY_STAGE_PUBLICATION_CONFLICT.getCode();
    }

    /**
     * 校验展台仍是调用方读取到的可编辑版本。
     *
     * @param stage           当前展台
     * @param expectedVersion 调用方读取到的版本
     * @throws BusinessException 展台不再是草稿或版本已经变化时抛出
     */
    private void ensureDraftAndVersion(ActivityStageEntity stage, int expectedVersion) {
        if (!stage.canEdit()) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_STAGE_STATE_ERROR,
                    stage.stageId(), stage.state().getCode());
        }
        if (stage.version() != expectedVersion) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_STAGE_CONFLICT,
                    stage.stageId(), expectedVersion, stage.version());
        }
    }

    /**
     * 校验关联活动当前允许对外发布。
     *
     * @param activityId 活动业务标识
     * @throws BusinessException 活动未开放时抛出
     */
    private void ensureActivityCanPublish(Long activityId) {
        ActivityEntity activity = activityRepository.getActivityEntityByIdActivityId(activityId);
        if (!ActivityStateVO.OPEN.getCode().equals(activity.state())) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_STATE_ERROR, activityId, activity.state());
        }
    }

    /**
     * 校验活动全部运行配置已经满足发布条件。
     *
     * @param activityId 活动业务标识
     * @throws BusinessException 活动存在阻断性配置问题时抛出
     */
    private void ensureActivityReadyToPublish(Long activityId) {
        ActivityReadinessResult readiness = activityReadinessApplicationService.check(activityId);
        if (!readiness.ready()) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_DEFINITION_NOT_READY,
                    activityId, readiness.errors().size(), readiness.errors().get(0).code());
        }
    }

    /**
     * 查询必然存在的展台。
     *
     * @param stageId 展台业务标识
     * @return 展台领域实体
     * @throws NullPointerException 展台标识为空时抛出
     * @throws BusinessException    展台不存在时抛出
     */
    private ActivityStageEntity queryStageRequired(Long stageId) {
        Long validStageId = Objects.requireNonNull(stageId, "stageId must not be null");
        return activityStageRepository.queryByStageId(validStageId)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.ACTIVITY_STAGE_NOT_FOUND, validStageId));
    }

    /**
     * 查询必然存在的展台发布任务。
     *
     * @param stageId      展台业务标识
     * @param stageVersion 展台版本
     * @return 发布任务领域实体
     * @throws NullPointerException     展台标识为空时抛出
     * @throws IllegalArgumentException 展台版本小于 1 时抛出
     * @throws BusinessException        发布任务不存在时抛出
     */
    private ActivityStagePublicationEntity queryPublicationRequired(Long stageId, int stageVersion) {
        Long validStageId = Objects.requireNonNull(stageId, "stageId must not be null");
        if (stageVersion < 1) {
            throw new IllegalArgumentException("stageVersion must be greater than zero");
        }
        return activityStagePublicationRepository.queryByStageAndVersion(validStageId, stageVersion)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.ACTIVITY_STAGE_PUBLICATION_NOT_FOUND,
                        validStageId, stageVersion));
    }

    /**
     * 校验并规范化运营人员标识。
     *
     * @param operator 运营人员标识
     * @return 去除首尾空格后的运营人员标识
     * @throws BusinessException 标识为空或长度超过 64 时抛出
     */
    private String requireOperator(String operator) {
        if (operator == null || operator.isBlank()) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_CONFIG_INVALID, operator);
        }
        String normalized = operator.trim();
        if (normalized.length() > 64) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_CONFIG_INVALID, operator);
        }
        return normalized;
    }

    /**
     * 将领域发布任务转换为应用层查询结果。
     *
     * @param publication 发布任务领域实体
     * @return 不暴露领域枚举的应用结果
     */
    private ActivityStagePublicationResult toResult(ActivityStagePublicationEntity publication) {
        return new ActivityStagePublicationResult(
                publication.publicationId(), publication.stageId(), publication.stageVersion(),
                publication.activityId(), publication.channel(), publication.source(),
                publication.state().getCode(), publication.phase().getCode(), publication.attemptCount(),
                publication.operator(), publication.failureCode(), publication.failureReason(),
                publication.publishedAt(), publication.createTime(), publication.updateTime());
    }

    /**
     * 发布任务准备结果。
     *
     * @param publication 当前发布任务
     * @param executable  当前请求是否取得任务执行权
     */
    private record PublicationAttempt(ActivityStagePublicationEntity publication, boolean executable) {
    }

    /**
     * 可安全写入数据库的发布失败信息。
     *
     * @param code   稳定错误编码
     * @param reason 面向运营人员的安全默认文案
     */
    private record PublicationFailure(String code, String reason) {
    }
}
