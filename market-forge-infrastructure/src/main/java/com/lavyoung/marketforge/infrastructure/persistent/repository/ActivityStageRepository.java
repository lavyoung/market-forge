package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStageEntity;
import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStageStateChange;
import com.lavyoung.marketforge.domain.activity.repository.IActivityStageRepository;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivityStageAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivityStageDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.ActivityStageStateUpdateParam;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityStagePO;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 活动展台仓储实现。
 *
 * <p>负责展台领域实体和 MyBatis 持久化对象之间的转换，并提供发布事务需要的渠道行锁、
 * 乐观锁条件更新和唯一键异常翻译。该仓储不负责活动配置校验或 Redis 预热。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Repository
@RequiredArgsConstructor
public class ActivityStageRepository implements IActivityStageRepository {

    private final IActivityStageDao activityStageDao;
    private final ActivityStageAssembler activityStageAssembler;

    @Override
    public Optional<ActivityStageEntity> queryByStageId(Long stageId) {
        Objects.requireNonNull(stageId, "stageId must not be null");
        return activityStageDao.queryByStageId(stageId).map(activityStageAssembler::toEntity);
    }

    @Override
    public Optional<ActivityStageEntity> queryActive(String channel, String source) {
        requireSc(channel, source);
        List<ActivityStagePO> activeStages = activityStageDao.queryActive(channel, source);
        if (activeStages.size() > 1) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_CONFIG_INVALID, channel, source, activeStages.size());
        }
        return activeStages.stream().findFirst().map(activityStageAssembler::toEntity);
    }

    @Override
    public List<ActivityStageEntity> lockByChannelAndSource(String channel, String source) {
        requireSc(channel, source);
        return List.copyOf(activityStageAssembler.toEntities(activityStageDao.lockByChannelAndSource(channel, source)));
    }

    @Override
    public void create(ActivityStageEntity stage) {
        Objects.requireNonNull(stage, "stage must not be null");
        ActivityStagePO stagePO = activityStageAssembler.toPO(stage);
        try {
            if (activityStageDao.insert(stagePO) != 1) {
                throw BusinessException.of(BusinessResponseCode.ACTIVITY_STAGE_CREATE_FAILED, stage.stageId());
            }
        } catch (DuplicateKeyException exception) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_STAGE_DUPLICATED, exception,
                    stage.channel(), stage.source(), stage.activityId());
        }
    }

    @Override
    public boolean updateDraft(ActivityStageEntity stage) {
        Objects.requireNonNull(stage, "stage must not be null");
        try {
            return activityStageDao.updateDraft(activityStageAssembler.toPO(stage)) == 1;
        } catch (DuplicateKeyException exception) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_STAGE_DUPLICATED, exception,
                    stage.channel(), stage.source(), stage.activityId());
        }
    }

    @Override
    public boolean changeState(ActivityStageStateChange stateChange) {
        Objects.requireNonNull(stateChange, "stateChange must not be null");
        ActivityStageStateUpdateParam param = new ActivityStageStateUpdateParam(
                stateChange.stageId(), stateChange.currentState().getCode(), stateChange.targetState().getCode(),
                stateChange.expectedVersion(), stateChange.operator());
        return activityStageDao.changeState(param) == 1;
    }

    private void requireSc(String channel, String source) {
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(source, "source must not be null");
    }
}
