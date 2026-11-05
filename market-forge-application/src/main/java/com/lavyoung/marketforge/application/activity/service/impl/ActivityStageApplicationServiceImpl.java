package com.lavyoung.marketforge.application.activity.service.impl;

import com.lavyoung.marketforge.application.activity.model.*;
import com.lavyoung.marketforge.application.activity.service.IActivityRaffleApplicationService;
import com.lavyoung.marketforge.application.activity.service.IActivityReadinessApplicationService;
import com.lavyoung.marketforge.application.activity.service.IActivityStageApplicationService;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStageEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStageStateVO;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStateVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityRepository;
import com.lavyoung.marketforge.domain.activity.repository.IActivityStageRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.utils.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 活动展台应用服务默认实现。
 *
 * <p>创建和修改只维护待上架配置。发布时先校验活动自身处于开放状态，再调用现有活动装配
 * 用例预热活动 SKU、次数配置、抽奖策略和奖品库存，最后通过独立事务原子切换生效配置。</p>
 *
 * <p>预热刻意放在数据库事务之前，避免 Redis 和策略构建耗时占用数据库连接及行锁。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Service
@RequiredArgsConstructor
public class ActivityStageApplicationServiceImpl implements IActivityStageApplicationService {

    private static final int STAGE_ID_LENGTH = 18;

    private final IActivityStageRepository activityStageRepository;
    private final IActivityRepository activityRepository;
    private final IActivityReadinessApplicationService activityReadinessApplicationService;
    private final IActivityRaffleApplicationService activityRaffleApplicationService;
    private final ActivityStagePublicationTransaction publicationTransaction;

    @Override
    public ActivityStageResult create(CreateActivityStageCommand command) {
        CreateActivityStageCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        Long stageId = Long.valueOf(IdGenerator.nextNumericId(STAGE_ID_LENGTH));
        ActivityStageEntity stage = createNewStage(stageId, validCommand);
        activityStageRepository.create(stage);
        return toResult(queryRequired(stageId));
    }

    @Override
    public ActivityStageResult queryByStageId(Long stageId) {
        return toResult(queryRequired(stageId));
    }

    @Override
    public ActivityStageResult updateDraft(UpdateActivityStageCommand command) {
        UpdateActivityStageCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        ActivityStageEntity current = queryRequired(validCommand.stageId());
        ensureDraftAndVersion(current, validCommand.expectedVersion());
        ActivityStageEntity updated = createUpdatedStage(current, validCommand);
        if (!activityStageRepository.updateDraft(updated)) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_STAGE_CONFLICT,
                    current.stageId(), validCommand.expectedVersion());
        }
        return toResult(queryRequired(current.stageId()));
    }

    /**
     * 预热并发布指定活动展台。
     *
     * <p>发布流程按以下顺序执行：</p>
     *
     * <ol>
     *     <li>校验操作人，确保所有发布副作用都有明确审计主体。</li>
     *     <li>读取展台草稿并校验生命周期状态和乐观锁版本。</li>
     *     <li>确认关联活动处于开放状态。</li>
     *     <li>重新检查活动、次数配置、SKU、策略和奖品是否满足运行条件。</li>
     *     <li>预热活动及抽奖策略运行时缓存。</li>
     *     <li>通过独立数据库事务失效旧展台并激活目标展台。</li>
     * </ol>
     *
     * <p>就绪检查和缓存预热均位于展台切换事务之前。检查或预热失败时不会调用
     * 发布事务，因此当前已经生效的展台保持不变，也不会长时间占用数据库连接
     * 和行锁。</p>
     *
     * @param command 展台发布命令
     * @return 发布完成后的展台配置
     * @throws NullPointerException 命令为空时抛出
     * @throws BusinessException    展台不存在、状态错误、版本冲突、活动未开放、
     *                              配置尚未就绪、预热失败或事务切换失败时抛出
     */
    @Override
    public ActivityStageResult publish(PublishActivityStageCommand command) {
        PublishActivityStageCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        String operator = requireOperator(validCommand.operator());
        ActivityStageEntity stage = queryRequired(validCommand.stageId());
        ensureDraftAndVersion(stage, validCommand.expectedVersion());
        ensureActivityCanPublish(stage.activityId());
        ensureActivityReadyToPublish(stage.activityId());

        activityRaffleApplicationService.armory(stage.activityId());
        publicationTransaction.publish(stage, validCommand.expectedVersion(), operator);
        return toResult(queryRequired(stage.stageId()));
    }

    @Override
    public Long queryActiveActivityId(String channel, String source) {
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(source, "source must not be null");
        return activityStageRepository.queryActive(channel, source)
                .map(ActivityStageEntity::activityId)
                .orElseThrow(() -> BusinessException.of(
                        BusinessResponseCode.ACTIVITY_STAGE_ACTIVE_NOT_FOUND, channel, source));
    }

    private ActivityStageEntity createNewStage(Long stageId, CreateActivityStageCommand command) {
        try {
            return new ActivityStageEntity(
                    stageId, command.channel(), command.source(), command.activityId(), ActivityStageStateVO.CREATE,
                    1, command.operator(), command.operator(), null, null);
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_CONFIG_INVALID, exception,
                    command.channel(), command.source(), command.activityId());
        }
    }

    private ActivityStageEntity createUpdatedStage(ActivityStageEntity current, UpdateActivityStageCommand command) {
        try {
            return new ActivityStageEntity(
                    current.stageId(), command.channel(), command.source(), command.activityId(), current.state(),
                    command.expectedVersion(), current.createdBy(), command.operator(), current.createTime(),
                    current.updateTime());
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_CONFIG_INVALID, exception,
                    current.stageId(), command.channel(), command.source(), command.activityId());
        }
    }

    private ActivityStageEntity queryRequired(Long stageId) {
        Long validStageId = Objects.requireNonNull(stageId, "stageId must not be null");
        return activityStageRepository.queryByStageId(validStageId)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.ACTIVITY_STAGE_NOT_FOUND, validStageId));
    }

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

    private void ensureActivityCanPublish(Long activityId) {
        ActivityEntity activity = activityRepository.getActivityEntityByIdActivityId(activityId);
        if (!ActivityStateVO.OPEN.getCode().equals(activity.state())) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_STATE_ERROR, activityId, activity.state());
        }
    }

    /**
     * 确认开放活动的全部运行配置仍然满足展台发布条件。
     *
     * <p>即使活动开放时已经执行过一次就绪检查，展台发布前仍然需要重新检查。
     * 该防线可以覆盖历史数据、人工修改数据库以及旧版本服务产生的不完整开放活动，
     * 同时防止调用方绕过运营后台直接请求发布接口。</p>
     *
     * <p>完整问题列表由就绪检查接口提供给运营后台。本方法只负责阻断发布，
     * 因此业务异常中记录活动标识、错误数量和首个稳定问题编码。</p>
     *
     * @param activityId 准备发布到展台的活动业务标识
     * @throws BusinessException 活动存在阻断性配置错误时抛出
     * @throws RuntimeException  就绪检查依赖的数据库或其他系统资源不可用时抛出
     */
    private void ensureActivityReadyToPublish(Long activityId) {
        ActivityReadinessResult readiness = activityReadinessApplicationService.check(activityId);
        if (!readiness.ready()) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_DEFINITION_NOT_READY,
                    activityId, readiness.errors().size(), readiness.errors().get(0).code());
        }
    }

    private String requireOperator(String operator) {
        if (operator == null || operator.isBlank() || operator.length() > 64) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_CONFIG_INVALID, operator);
        }
        return operator;
    }

    private ActivityStageResult toResult(ActivityStageEntity stage) {
        return new ActivityStageResult(
                stage.stageId(), stage.channel(), stage.source(), stage.activityId(), stage.state().getCode(),
                stage.version(), stage.createdBy(), stage.updatedBy(), stage.createTime(), stage.updateTime());
    }
}
