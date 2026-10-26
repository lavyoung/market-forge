package com.lavyoung.marketforge.application.activity.service.impl;

import com.lavyoung.marketforge.application.activity.model.ActivityCountDefinitionResult;
import com.lavyoung.marketforge.application.activity.model.CreateActivityCountDefinitionCommand;
import com.lavyoung.marketforge.application.activity.model.UpdateActivityCountDefinitionCommand;
import com.lavyoung.marketforge.application.activity.service.IActivityCountDefinitionApplicationService;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.repository.IActivityCountDefinitionRepository;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.utils.IdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * 活动次数配置运营应用服务默认实现。
 *
 * <p>创建时生成独立业务标识；修改时先提供明确的开放活动引用提示，再依赖仓储中的
 * 条件更新同时守住引用关系和乐观锁版本，避免并发请求覆盖配置。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
@Service
@RequiredArgsConstructor
public class ActivityCountDefinitionApplicationServiceImpl implements IActivityCountDefinitionApplicationService {

    private static final int ACTIVITY_COUNT_ID_LENGTH = 18;

    private final IActivityCountDefinitionRepository activityCountDefinitionRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityCountDefinitionResult create(CreateActivityCountDefinitionCommand command) {
        CreateActivityCountDefinitionCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        Long activityCountId = Long.valueOf(IdGenerator.nextNumericId(ACTIVITY_COUNT_ID_LENGTH));
        ActivityCountDefinitionEntity definition = createDefinition(activityCountId, validCommand);
        activityCountDefinitionRepository.create(definition);
        return toResult(queryRequired(activityCountId));
    }

    @Override
    @Transactional(readOnly = true)
    public ActivityCountDefinitionResult queryByActivityCountId(Long activityCountId) {
        return toResult(queryRequired(activityCountId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActivityCountDefinitionResult update(UpdateActivityCountDefinitionCommand command) {
        UpdateActivityCountDefinitionCommand validCommand = Objects.requireNonNull(command, "command must not be null");
        ActivityCountDefinitionEntity current = queryRequired(validCommand.activityCountId());
        ensureExpectedVersion(current, validCommand.expectedVersion());
        if (activityCountDefinitionRepository.existsOpenActivityReference(current.activityCountId())) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_COUNT_DEFINITION_REFERENCED, current.activityCountId());
        }
        ActivityCountDefinitionEntity updated = updateDefinition(current, validCommand);
        if (!activityCountDefinitionRepository.updateIfNotReferencedByOpenActivity(updated)) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_COUNT_DEFINITION_CONFLICT,
                    current.activityCountId(), validCommand.expectedVersion());
        }
        return toResult(queryRequired(current.activityCountId()));
    }

    private ActivityCountDefinitionEntity createDefinition(Long activityCountId, CreateActivityCountDefinitionCommand command) {
        try {
            return new ActivityCountDefinitionEntity(activityCountId, command.activityCountName(), command.totalCount(),
                    command.dayCount(), command.monthCount(), 1, command.operator(), command.operator(), null, null);
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_COUNT_INVALID, exception, command.activityCountName());
        }
    }

    private ActivityCountDefinitionEntity updateDefinition(ActivityCountDefinitionEntity current, UpdateActivityCountDefinitionCommand command) {
        try {
            return current.update(command.activityCountName(), command.totalCount(), command.dayCount(),
                    command.monthCount(), command.operator());
        } catch (NullPointerException | IllegalArgumentException exception) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_COUNT_INVALID, exception, current.activityCountId());
        }
    }

    private ActivityCountDefinitionEntity queryRequired(Long activityCountId) {
        Long validActivityCountId = Objects.requireNonNull(activityCountId, "activityCountId must not be null");
        return activityCountDefinitionRepository.queryByActivityCountId(validActivityCountId)
                .orElseThrow(() -> BusinessException.of(BusinessResponseCode.ACTIVITY_COUNT_NOT_FOUND, validActivityCountId));
    }

    private void ensureExpectedVersion(ActivityCountDefinitionEntity current, int expectedVersion) {
        if (current.version() != expectedVersion) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_COUNT_DEFINITION_CONFLICT,
                    current.activityCountId(), expectedVersion, current.version());
        }
    }

    private ActivityCountDefinitionResult toResult(ActivityCountDefinitionEntity entity) {
        return new ActivityCountDefinitionResult(entity.activityCountId(), entity.activityCountName(), entity.totalCount(),
                entity.dayCount(), entity.monthCount(), entity.version(), entity.createdBy(), entity.updatedBy(),
                entity.createTime(), entity.updateTime());
    }
}
