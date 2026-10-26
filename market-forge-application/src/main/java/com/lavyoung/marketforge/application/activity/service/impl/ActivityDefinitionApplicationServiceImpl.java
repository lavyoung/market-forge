package com.lavyoung.marketforge.application.activity.service.impl;

import com.lavyoung.marketforge.application.activity.model.ActivityDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.ChangeActivityDefinitionStateCommand;
import com.lavyoung.marketforge.application.activity.model.CreateActivityDefinitionCommand;
import com.lavyoung.marketforge.application.activity.model.UpdateActivityDefinitionCommand;
import com.lavyoung.marketforge.application.activity.service.IActivityDefinitionApplicationService;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityDefinitionStateChange;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityDefinitionStateVO;
import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import com.lavyoung.marketforge.domain.activity.repository.IActivityCountDefinitionRepository;
import com.lavyoung.marketforge.domain.activity.repository.IActivityDefinitionRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.utils.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 活动定义运营应用服务。
 *
 * <p>负责生成业务标识、构建领域实体、校验生命周期与版本，并把持久化条件更新失败
 * 转换为稳定的并发冲突。HTTP、Session 和 MyBatis 细节均不进入本类。</p>
 */
@Service
@RequiredArgsConstructor
public class ActivityDefinitionApplicationServiceImpl implements IActivityDefinitionApplicationService {
    private static final int ACTIVITY_ID_LENGTH = 18;

    private final IActivityDefinitionRepository activityDefinitionRepository;
    private final IActivityCountDefinitionRepository activityCountDefinitionRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityDefinitionResult create(CreateActivityDefinitionCommand command) {
        CreateActivityDefinitionCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        Long activityId = Long.valueOf(IdGenerator.nextNumericId(ACTIVITY_ID_LENGTH));
        ActivityDefinitionEntity definition = createDefinition(activityId, validCommand);
        activityDefinitionRepository.create(definition);
        return toResult(queryRequired(activityId));
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityDefinitionResult queryByActivityId(Long activityId) {
        return toResult(queryRequired(activityId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityDefinitionResult updateDraft(UpdateActivityDefinitionCommand command) {
        UpdateActivityDefinitionCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        ActivityDefinitionEntity current = queryRequired(validCommand.activityId());
        if (!current.canEdit()) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_DEFINITION_STATE_ERROR,
                    current.activityId(), current.state().getCode());
        }
        ensureExpectedVersion(current, validCommand.expectedVersion());
        ActivityDefinitionEntity updated = updateDefinition(current, validCommand);
        if (!activityDefinitionRepository.updateDraft(updated)) {
            throw conflict(current.activityId(), validCommand.expectedVersion());
        }
        return toResult(queryRequired(current.activityId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityDefinitionResult changeState(ChangeActivityDefinitionStateCommand command) {
        ChangeActivityDefinitionStateCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        ActivityDefinitionEntity current = queryRequired(validCommand.activityId());
        ensureExpectedVersion(current, validCommand.expectedVersion());
        ActivityDefinitionStateVO targetState = ActivityDefinitionStateVO.of(validCommand.targetState())
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.ACTIVITY_DEFINITION_STATE_ERROR,
                        validCommand.targetState()));
        if (!current.state().canTransitionTo(targetState)) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_DEFINITION_STATE_ERROR,
                    current.activityId(), current.state().getCode(), targetState.getCode());
        }
        if (targetState == ActivityDefinitionStateVO.OPEN) {
            activityCountDefinitionRepository.lockByActivityCountId(current.activityCountId())
                    .orElseThrow(() -> BusinessException.of(
                            BusinessResponseCode.ACTIVITY_COUNT_NOT_FOUND, current.activityCountId()));
        }
        ActivityDefinitionStateChange stateChange = new ActivityDefinitionStateChange(current.activityId(),
                current.state(), targetState, validCommand.expectedVersion(), validCommand.operator());
        if (!activityDefinitionRepository.changeState(stateChange)) {
            throw conflict(current.activityId(), validCommand.expectedVersion());
        }
        return toResult(queryRequired(current.activityId()));
    }

    private ActivityDefinitionEntity createDefinition(Long activityId, CreateActivityDefinitionCommand command) {
        try {
            AwardUnlockScopeVO scope = parseScope(command.awardUnlockScope());
            return new ActivityDefinitionEntity(activityId, command.activityName(), command.activityDesc(),
                    command.beginDateTime(), command.endDateTime(), command.stockCount(), command.stockCount(),
                    command.activityCountId(), command.strategyId(), scope, ActivityDefinitionStateVO.DRAFT, 1,
                    command.operator(), command.operator(), null, null);
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_CONFIG_INVALID, exception, command.activityName());
        }
    }

    private ActivityDefinitionEntity updateDefinition(ActivityDefinitionEntity current,
                                                      UpdateActivityDefinitionCommand command) {
        try {
            return new ActivityDefinitionEntity(current.activityId(), command.activityName(), command.activityDesc(),
                    command.beginDateTime(), command.endDateTime(), command.stockCount(), command.stockCount(),
                    command.activityCountId(), command.strategyId(), parseScope(command.awardUnlockScope()),
                    current.state(), command.expectedVersion(), current.createdBy(), command.operator(),
                    current.createTime(), current.updateTime());
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_CONFIG_INVALID, exception, current.activityId());
        }
    }

    private AwardUnlockScopeVO parseScope(String code) {
        return AwardUnlockScopeVO.fromCode(code)
                .orElseThrow(() -> new IllegalArgumentException("unknown award unlock scope: " + code));
    }

    private ActivityDefinitionEntity queryRequired(Long activityId) {
        Long validActivityId = Objects.requireNonNull(activityId, "activityId must not be null");
        return activityDefinitionRepository.queryByActivityId(validActivityId)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.LOTTERY_ACTIVITY_NOT_FOUND, validActivityId));
    }

    private void ensureExpectedVersion(ActivityDefinitionEntity current, int expectedVersion) {
        if (current.version() != expectedVersion) {
            throw conflict(current.activityId(), expectedVersion);
        }
    }

    private BusinessException conflict(Long activityId, int expectedVersion) {
        return BusinessException.of(BusinessResponseCode.ACTIVITY_DEFINITION_CONFLICT, activityId, expectedVersion);
    }

    private ActivityDefinitionResult toResult(ActivityDefinitionEntity entity) {
        return new ActivityDefinitionResult(entity.activityId(), entity.activityName(), entity.activityDesc(),
                entity.beginDateTime(), entity.endDateTime(), entity.stockCount(), entity.stockCountSurplus(),
                entity.activityCountId(), entity.strategyId(), entity.awardUnlockScope().getCode(),
                entity.state().getCode(), entity.version(), entity.createdBy(), entity.updatedBy(),
                entity.createTime(), entity.updateTime());
    }
}
