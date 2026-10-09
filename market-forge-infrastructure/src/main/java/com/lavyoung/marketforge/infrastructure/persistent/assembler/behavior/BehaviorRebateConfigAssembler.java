package com.lavyoung.marketforge.infrastructure.persistent.assembler.behavior;

import com.lavyoung.marketforge.domain.behavior.model.entity.BehaviorRebateConfigEntity;
import com.lavyoung.marketforge.domain.behavior.model.vo.BehaviorTypeVO;
import com.lavyoung.marketforge.domain.behavior.model.vo.RebateTypeVO;
import com.lavyoung.marketforge.infrastructure.persistent.po.behavior.BehaviorRebateConfigPO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 行为返利配置持久化对象与领域实体转换器。
 *
 * <p>数据库中行为类型和返利类型以字符串编码保存，领域层使用枚举表达业务语义。
 * 该转换器负责完成编码和枚举之间的映射，避免仓储实现中散落重复转换逻辑。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/08
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BehaviorRebateConfigAssembler {

    /**
     * 将行为返利配置持久化对象转换为领域实体。
     *
     * @param source 行为返利配置持久化对象
     * @return 行为返利配置领域实体
     */
    BehaviorRebateConfigEntity toEntity(BehaviorRebateConfigPO source);

    /**
     * 批量将行为返利配置持久化对象转换为领域实体。
     *
     * @param sources 行为返利配置持久化对象列表
     * @return 行为返利配置领域实体列表
     */
    List<BehaviorRebateConfigEntity> toEntities(List<BehaviorRebateConfigPO> sources);

    /**
     * 将数据库行为类型编码转换为领域枚举。
     *
     * @param code 行为类型编码，例如 {@code sign}
     * @return 行为类型枚举
     * @throws IllegalArgumentException 当编码未定义时抛出
     */
    default BehaviorTypeVO toBehaviorTypeVO(String code) {
        return BehaviorTypeVO.fromCode(code).orElseThrow(() -> new IllegalArgumentException("unknown behaviorType: " + code));
    }

    /**
     * 将数据库返利类型编码转换为领域枚举。
     *
     * @param code 返利类型编码，例如 {@code sku}、{@code integral}
     * @return 返利类型枚举
     * @throws IllegalArgumentException 当编码未定义时抛出
     */
    default RebateTypeVO toRebateTypeVO(String code) {
        return RebateTypeVO.fromCode(code).orElseThrow(() -> new IllegalArgumentException("unknown rebateType: " + code));
    }
}
