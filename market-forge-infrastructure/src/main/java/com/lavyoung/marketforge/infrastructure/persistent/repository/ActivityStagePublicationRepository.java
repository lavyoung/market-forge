package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStagePublicationEntity;
import com.lavyoung.marketforge.domain.activity.repository.IActivityStagePublicationRepository;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivityStagePublicationAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivityStagePublicationDao;
import com.lavyoung.marketforge.infrastructure.persistent.model.ActivityStagePublicationTransitionParam;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

/**
 * 活动展台发布记录仓储实现。
 *
 * <p>负责领域发布记录和数据库模型之间的转换，并通过唯一键和条件更新提供
 * 幂等创建及并发安全的状态迁移。发布阶段是否合法仍由领域实体负责判断，
 * Repository 不重复实现状态机。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/06
 */
@Repository
@RequiredArgsConstructor
public class ActivityStagePublicationRepository implements IActivityStagePublicationRepository {

    private final IActivityStagePublicationDao activityStagePublicationDao;
    private final ActivityStagePublicationAssembler activityStagePublicationAssembler;

    @Override
    public Optional<ActivityStagePublicationEntity> queryByStageAndVersion(Long stageId, int stageVersion) {
        Objects.requireNonNull(stageId, "stageId must not be null");
        if (stageVersion < 1) {
            throw new IllegalArgumentException("stageVersion must be greater than zero");
        }
        return activityStagePublicationDao.queryByStageAndVersion(stageId, stageVersion)
                .map(activityStagePublicationAssembler::toEntity);
    }

    @Override
    public boolean createIfAbsent(ActivityStagePublicationEntity publication) {
        Objects.requireNonNull(publication, "publication must not be null");
        try {
            if (activityStagePublicationDao.insert(activityStagePublicationAssembler.toPO(publication)) == 1) {
                return true;
            }
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_STAGE_PUBLICATION_CREATE_FAILED,
                    publication.publicationId());
        } catch (DuplicateKeyException exception) {
            if (queryByStageAndVersion(publication.stageId(), publication.stageVersion()).isPresent()) {
                return false;
            }
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_STAGE_PUBLICATION_ID_CONFLICT,
                    exception, publication.publicationId());
        } catch (DataAccessException exception) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_STAGE_PUBLICATION_CREATE_FAILED,
                    exception, publication.publicationId());
        }
    }

    @Override
    public boolean transition(ActivityStagePublicationEntity current, ActivityStagePublicationEntity target) {
        Objects.requireNonNull(current, "current publication must not be null");
        Objects.requireNonNull(target, "target publication must not be null");
        requireSamePublication(current, target);

        ActivityStagePublicationTransitionParam transition = new ActivityStagePublicationTransitionParam(
                current.publicationId(), current.state().getCode(), current.phase().getCode(), current.attemptCount(),
                target.state().getCode(), target.phase().getCode(), target.attemptCount(), target.operator(),
                target.failureCode(), target.failureReason(), target.publishedAt(), target.updateTime());

        try {
            return activityStagePublicationDao.transition(transition) == 1;
        } catch (DataAccessException exception) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_STAGE_PUBLICATION_UPDATE_FAILED,
                    exception, current.publicationId());
        }
    }

    /**
     * 校验迁移前后的记录属于同一个发布任务和配置快照。
     *
     * <p>该校验不是发布状态机判断，而是 Repository 的身份保护，防止调用方
     * 错误地把两个不同发布任务拼成一次条件更新。</p>
     *
     * @param current 更新前发布记录
     * @param target  更新后发布记录
     * @throws IllegalArgumentException 两个快照不属于同一发布任务时抛出
     */
    private void requireSamePublication(ActivityStagePublicationEntity current, ActivityStagePublicationEntity target) {
        boolean samePublication = Objects.equals(current.publicationId(), target.publicationId())
                && Objects.equals(current.stageId(), target.stageId())
                && current.stageVersion() == target.stageVersion()
                && Objects.equals(current.activityId(), target.activityId())
                && Objects.equals(current.channel(), target.channel())
                && Objects.equals(current.source(), target.source())
                && Objects.equals(current.createTime(), target.createTime());

        if (!samePublication) {
            throw new IllegalArgumentException("publication transition must preserve task identity and snapshot");
        }
    }
}
