package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityDefinitionStateChange;
import com.lavyoung.marketforge.domain.activity.repository.IActivityDefinitionRepository;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivityDefinitionAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivityDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.ActivityDefinitionStateUpdateParam;
import com.lavyoung.marketforge.infrastructure.persistent.redis.IRedisService;
import com.lavyoung.marketforge.types.common.Constants;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

/**
 * 活动定义仓储实现。
 *
 * <p>负责活动运营写模型持久化、唯一键异常转换、乐观锁条件更新以及运行时缓存失效。</p>
 */
@Repository
@RequiredArgsConstructor
public class ActivityDefinitionRepository implements IActivityDefinitionRepository {
    private final IActivityDao activityDao;
    private final ActivityDefinitionAssembler activityDefinitionAssembler;
    private final IRedisService redisService;

    @Override
    public Optional<ActivityDefinitionEntity> queryByActivityId(Long activityId) {
        Objects.requireNonNull(activityId, "activityId must not be null");
        return activityDao.queryByActivityId(activityId).map(activityDefinitionAssembler::toEntity);
    }

    @Override
    public void create(ActivityDefinitionEntity definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        try {
            if (activityDao.insert(activityDefinitionAssembler.toPO(definition)) != 1) {
                throw BusinessException.of(BusinessResponseCode.ACTIVITY_DEFINITION_CREATE_FAILED,
                        definition.activityId());
            }
        } catch (DuplicateKeyException exception) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_DEFINITION_DUPLICATED,
                    exception, definition.activityId());
        }
    }

    @Override
    public boolean updateDraft(ActivityDefinitionEntity definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        boolean updated = activityDao.updateDraftDefinition(activityDefinitionAssembler.toPO(definition)) == 1;
        if (updated) {
            evictActivity(definition.activityId());
        }
        return updated;
    }

    @Override
    public boolean changeState(ActivityDefinitionStateChange stateChange) {
        Objects.requireNonNull(stateChange, "stateChange must not be null");
        ActivityDefinitionStateUpdateParam param = new ActivityDefinitionStateUpdateParam(stateChange.activityId(),
                stateChange.currentState().getCode(), stateChange.targetState().getCode(),
                stateChange.expectedVersion(), stateChange.operator());
        boolean updated = activityDao.changeDefinitionState(param) == 1;
        if (updated) {
            evictActivity(stateChange.activityId());
        }
        return updated;
    }

    private void evictActivity(Long activityId) {
        redisService.delete(Constants.RedisKeys.ACTIVITY_DETAIL_KEY + activityId);
    }
}
