package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityDefinitionEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityDefinitionStateVO;
import com.lavyoung.marketforge.domain.activity.model.vo.AwardUnlockScopeVO;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityPO;
import org.mapstruct.*;

/**
 * 活动定义领域实体与持久化对象装配器。
 *
 * <p>负责数据库编码与领域状态、解锁范围之间的严格转换，未知编码立即失败。</p>
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ActivityDefinitionAssembler {
    /**
     * 将持久化对象转换为活动定义。
     *
     * @param source 数据库活动记录
     * @return 活动定义
     */
    @Mapping(target = "awardUnlockScope", source = "awardUnlockScope", qualifiedByName = "toUnlockScope")
    @Mapping(target = "state", source = "state", qualifiedByName = "toState")
    ActivityDefinitionEntity toEntity(ActivityPO source);

    /**
     * 将活动定义转换为持久化对象。
     *
     * @param source 活动定义
     * @return 持久化对象
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "awardUnlockScope", source = "awardUnlockScope", qualifiedByName = "fromUnlockScope")
    @Mapping(target = "state", source = "state", qualifiedByName = "fromState")
    ActivityPO toPO(ActivityDefinitionEntity source);

    @Named("toUnlockScope")
    default AwardUnlockScopeVO toUnlockScope(String code) {
        return AwardUnlockScopeVO.fromCode(code)
                .orElseThrow(() -> new IllegalArgumentException("unknown award unlock scope: " + code));
    }

    @Named("fromUnlockScope")
    default String fromUnlockScope(AwardUnlockScopeVO scope) {
        return scope.getCode();
    }

    @Named("toState")
    default ActivityDefinitionStateVO toState(String code) {
        return ActivityDefinitionStateVO.of(code)
                .orElseThrow(() -> new IllegalArgumentException("unknown activity definition state: " + code));
    }

    @Named("fromState")
    default String fromState(ActivityDefinitionStateVO state) {
        return state.getCode();
    }
}
