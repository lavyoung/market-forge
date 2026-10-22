package com.lavyoung.marketforge.infrastructure.persistent.assembler.activity;

import com.lavyoung.marketforge.domain.activity.model.entity.ActivityStageEntity;
import com.lavyoung.marketforge.domain.activity.model.vo.ActivityStageStateVO;
import com.lavyoung.marketforge.infrastructure.persistent.po.activity.ActivityStagePO;
import org.mapstruct.*;

import java.util.List;

/**
 * 活动展台领域实体与持久化对象转换器。
 *
 * <p>负责隔离数据库状态编码和领域生命周期枚举。数据库出现未知状态时立即失败，
 * 防止损坏的投放配置继续进入发布流程。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ActivityStageAssembler {

    /**
     * 将展台持久化对象转换为领域实体。
     *
     * @param source 展台持久化对象
     * @return 展台领域实体；输入为空时返回空
     * @throws IllegalArgumentException 数据库状态编码无法识别时抛出
     */
    @Mapping(target = "state", source = "state", qualifiedByName = "toStageState")
    ActivityStageEntity toEntity(ActivityStagePO source);

    /**
     * 批量转换展台持久化对象。
     *
     * @param sources 展台持久化对象集合
     * @return 展台领域实体集合
     */
    List<ActivityStageEntity> toEntities(List<ActivityStagePO> sources);

    /**
     * 将展台领域实体转换为持久化对象。
     *
     * @param source 展台领域实体
     * @return 展台持久化对象
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "state", source = "state", qualifiedByName = "fromStageState")
    ActivityStagePO toPO(ActivityStageEntity source);

    /**
     * 将数据库状态编码解析为领域状态。
     *
     * @param code 状态编码
     * @return 展台生命周期状态
     * @throws IllegalArgumentException 编码无法识别时抛出
     */
    @Named("toStageState")
    default ActivityStageStateVO toStageState(String code) {
        return ActivityStageStateVO.of(code)
                .orElseThrow(() -> new IllegalArgumentException("unknown activity stage state: " + code));
    }

    /**
     * 提取领域状态的持久化编码。
     *
     * @param state 展台生命周期状态
     * @return 数据库存储编码
     */
    @Named("fromStageState")
    default String fromStageState(ActivityStageStateVO state) {
        return state.getCode();
    }
}
