package com.lavyoung.marketforge.application.activity.service.impl;

import com.lavyoung.marketforge.application.activity.model.ActivityStageResult;
import com.lavyoung.marketforge.application.activity.model.CreateActivityStageCommand;
import com.lavyoung.marketforge.application.activity.model.PublishActivityStageCommand;
import com.lavyoung.marketforge.application.activity.model.UpdateActivityStageCommand;
import com.lavyoung.marketforge.application.activity.service.IActivityRaffleApplicationService;
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

    @Override
    public ActivityStageResult publish(PublishActivityStageCommand command) {
        PublishActivityStageCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        String operator = requireOperator(validCommand.operator());
        ActivityStageEntity stage = queryRequired(validCommand.stageId());
        ensureDraftAndVersion(stage, validCommand.expectedVersion());
        ensureActivityCanPublish(stage.activityId());

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
