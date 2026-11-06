package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStagePublicationEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStagePublicationPhaseVO;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStagePublicationStateVO;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityStagePublicationPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

/**
 * 活动展台发布记录领域实体与持久化对象转换器。
 *
 * <p>负责隔离领域枚举与数据库字符串编码。数据库中的未知状态或阶段必须立即暴露，
 * 不能把损坏的发布记录继续传入发布流程。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/06
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ActivityStagePublicationAssembler {

    /**
     * 将发布记录持久化对象转换为领域实体。
     *
     * @param source 发布记录持久化对象
     * @return 发布记录领域实体；输入为空时返回空
     * @throws IllegalArgumentException 状态或阶段编码无法识别时抛出
     */
    @Mapping(target = "state", source = "state", qualifiedByName = "toPublicationState")
    @Mapping(target = "phase", source = "phase", qualifiedByName = "toPublicationPhase")
    ActivityStagePublicationEntity toEntity(ActivityStagePublicationPO source);

    /**
     * 将发布记录领域实体转换为持久化对象。
     *
     * @param source 发布记录领域实体
     * @return 可交给 MyBatis 持久化的对象
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "state", source = "state", qualifiedByName = "fromPublicationState")
    @Mapping(target = "phase", source = "phase", qualifiedByName = "fromPublicationPhase")
    ActivityStagePublicationPO toPO(ActivityStagePublicationEntity source);

    /**
     * 解析数据库中的发布状态编码。
     *
     * @param code 发布状态编码
     * @return 发布任务状态
     * @throws IllegalArgumentException 编码未知时抛出
     */
    @Named("toPublicationState")
    default ActivityStagePublicationStateVO toPublicationState(String code) {
        return ActivityStagePublicationStateVO.of(code)
                .orElseThrow(() -> new IllegalArgumentException("unknown activity stage publication state: " + code));
    }

    /**
     * 解析数据库中的发布阶段编码。
     *
     * @param code 发布阶段编码
     * @return 发布执行阶段
     * @throws IllegalArgumentException 编码未知时抛出
     */
    @Named("toPublicationPhase")
    default ActivityStagePublicationPhaseVO toPublicationPhase(String code) {
        return ActivityStagePublicationPhaseVO.of(code)
                .orElseThrow(() -> new IllegalArgumentException("unknown activity stage publication phase: " + code));
    }

    /**
     * 提取发布状态的数据库编码。
     *
     * @param state 发布任务状态
     * @return 数据库存储编码
     */
    @Named("fromPublicationState")
    default String fromPublicationState(ActivityStagePublicationStateVO state) {
        return state.getCode();
    }

    /**
     * 提取发布阶段的数据库编码。
     *
     * @param phase 发布执行阶段
     * @return 数据库存储编码
     */
    @Named("fromPublicationPhase")
    default String fromPublicationPhase(ActivityStagePublicationPhaseVO phase) {
        return phase.getCode();
    }
}
