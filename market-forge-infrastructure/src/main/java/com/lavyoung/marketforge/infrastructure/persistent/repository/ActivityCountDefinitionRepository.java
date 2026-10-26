package com.lavyoung.marketforge.infrastructure.persistent.repository;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityCountDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.repository.IActivityCountDefinitionRepository;
import com.lavyoung.marketforge.infrastructure.persistent.assembler.activity.ActivityCountDefinitionAssembler;
import com.lavyoung.marketforge.infrastructure.persistent.dao.activity.IActivityCountDao;
import com.lavyoung.marketforge.types.exception.BusinessException;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;

/**
 * 活动次数配置运营仓储实现。
 *
 * <p>负责运营领域定义与持久化对象转换、唯一键异常翻译，并通过条件更新同时校验
 * 乐观锁版本和开放活动引用关系。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/26
 */
@Repository
@RequiredArgsConstructor
public class ActivityCountDefinitionRepository implements IActivityCountDefinitionRepository {

    private final IActivityCountDao activityCountDao;
    private final ActivityCountDefinitionAssembler activityCountDefinitionAssembler;

    @Override
    public Optional<ActivityCountDefinitionEntity> queryByActivityCountId(Long activityCountId) {
        Objects.requireNonNull(activityCountId, "activityCountId must not be null");
        return activityCountDao.queryByActivityCountId(activityCountId).map(activityCountDefinitionAssembler::toEntity);
    }

    @Override
    public Optional<ActivityCountDefinitionEntity> lockByActivityCountId(Long activityCountId) {
        Objects.requireNonNull(activityCountId, "activityCountId must not be null");
        return activityCountDao.lockByActivityCountId(activityCountId).map(activityCountDefinitionAssembler::toEntity);
    }

    @Override
    public void create(ActivityCountDefinitionEntity definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        try {
            if (activityCountDao.insert(activityCountDefinitionAssembler.toPO(definition)) != 1) {
                throw BusinessException.of(BusinessResponseCode.ACTIVITY_COUNT_DEFINITION_CREATE_FAILED,
                        definition.activityCountId());
            }
        } catch (DuplicateKeyException exception) {
            throw BusinessException.of(BusinessResponseCode.ACTIVITY_COUNT_DEFINITION_DUPLICATED,
                    exception, definition.activityCountId());
        }
    }

    @Override
    public boolean existsOpenActivityReference(Long activityCountId) {
        Objects.requireNonNull(activityCountId, "activityCountId must not be null");
        return activityCountDao.existsOpenActivityReference(activityCountId);
    }

    @Override
    public boolean updateIfNotReferencedByOpenActivity(ActivityCountDefinitionEntity definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        return activityCountDao.updateIfNotReferencedByOpenActivity(
                activityCountDefinitionAssembler.toPO(definition)) == 1;
    }
}
