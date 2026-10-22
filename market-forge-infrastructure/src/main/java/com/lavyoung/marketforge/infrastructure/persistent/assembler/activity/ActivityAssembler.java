package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 抽奖活动持久化对象与领域实体转换器。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/18
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ActivityAssembler {

    /**
     * 将活动持久化对象转换为领域实体。
     *
     * @param activityPO 活动持久化对象
     * @return 活动领域实体
     */
    ActivityEntity toEntity(ActivityPO activityPO);

    /**
     * 批量将活动持久化对象转换为领域实体。
     *
     * @param activityPOs 活动持久化对象列表
     * @return 活动领域实体列表
     */
    List<ActivityEntity> toEntities(List<ActivityPO> activityPOs);

    /**
     * 将活动领域实体转换为待持久化对象。
     *
     * @param activityEntity 活动领域实体
     * @return 活动持久化对象
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    ActivityPO toPO(ActivityEntity activityEntity);

    /**
     * 批量将活动领域实体转换为待持久化对象。
     *
     * @param activityEntities 活动领域实体列表
     * @return 活动持久化对象列表
     */
    List<ActivityPO> toPOs(List<ActivityEntity> activityEntities);

    /**
     * 将数据库范围编码转换为领域值对象。
     *
     * @param code 数据库存储的范围编码
     * @return 活动奖品解锁统计范围
     * @throws IllegalArgumentException 编码为空或无法识别时抛出
     */
    default AwardUnlockScopeVO toAwardUnlockScope(String code) {
        return AwardUnlockScopeVO.fromCode(code)
                .orElseThrow(() -> new IllegalArgumentException(
                        "unsupported award unlock scope: " + code
                ));
    }

    /**
     * 将领域值对象转换为数据库范围编码。
     *
     * @param scope 活动奖品解锁统计范围
     * @return 数据库存储编码；范围为空时返回 {@code null}
     */
    default String toAwardUnlockScopeCode(AwardUnlockScopeVO scope) {
        return scope == null ? null : scope.getCode();
    }
}
