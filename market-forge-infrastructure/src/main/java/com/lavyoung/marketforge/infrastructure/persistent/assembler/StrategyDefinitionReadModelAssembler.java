package com.lavyoung.marketforge.infrastructure.persistent.assembler;

import com.lavyoung.marketforge.application.strategy.model.StrategyDefinitionSearchItem;
import com.lavyoung.marketforge.infrastructure.persistent.po.StrategyPO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * 策略定义运营读模型装配器。
 *
 * <p>将策略主表持久化对象直接投影为 Application 列表项。
 * 分页查询不执行领域规则，因此不需要先构造完整策略聚合。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/25
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface StrategyDefinitionReadModelAssembler {

    /**
     * 批量转换策略持久化记录。
     *
     * @param sources 策略持久化对象列表
     * @return 策略定义列表项
     */
    List<StrategyDefinitionSearchItem> toItems(List<StrategyPO> sources);
}
