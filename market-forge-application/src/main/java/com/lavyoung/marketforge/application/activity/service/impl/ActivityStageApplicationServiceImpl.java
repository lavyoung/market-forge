package com.lavyoung.marketforge.application.activity.service.impl;

import com.lavyoung.marketforge.application.activity.model.ActivityStageResult;
import com.lavyoung.marketforge.application.activity.model.CreateActivityStageCommand;
import com.lavyoung.marketforge.application.activity.model.PublishActivityStageCommand;
import com.lavyoung.marketforge.application.activity.model.UpdateActivityStageCommand;
import com.lavyoung.marketforge.application.activity.service.IActivityStageApplicationService;
import com.lavyoung.marketforge.application.activity.service.IActivityStagePublicationApplicationService;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStageEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStageStateVO;
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
 * <p>负责活动展台草稿的创建、修改、查询和当前生效活动解析。发布过程由
 * {@link ActivityStagePublicationApplicationServiceImpl} 独立负责，避免展台维护服务同时承担
 * 幂等任务、配置检查、运行数据预热、状态迁移和失败留痕等多项职责。</p>
 *
 * <p>旧版发布方法暂时保留并委托新的发布应用服务，用于兼容尚未迁移的调用方。
 * 新增调用方不得继续依赖旧版发布结果。</p>
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
    private final IActivityStagePublicationApplicationService activityStagePublicationApplicationService;

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

    @Deprecated(since = "1.0.0", forRemoval = false)
    @Override
    public ActivityStageResult publish(PublishActivityStageCommand command) {
        PublishActivityStageCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        activityStagePublicationApplicationService.publish(validCommand);
        return toResult(queryRequired(validCommand.stageId()));
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

    private ActivityStageResult toResult(ActivityStageEntity stage) {
        return new ActivityStageResult(
                stage.stageId(), stage.channel(), stage.source(), stage.activityId(), stage.state().getCode(),
                stage.version(), stage.createdBy(), stage.updatedBy(), stage.createTime(), stage.updateTime());
    }
}
