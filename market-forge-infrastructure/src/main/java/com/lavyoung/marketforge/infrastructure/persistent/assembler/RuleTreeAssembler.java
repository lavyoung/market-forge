package com.lavyoung.marketforge.infrastructure.persistent.assembler;

import com.lavyoung.marketforge.domain.strategy.model.vo.RuleTreeVO;
import com.lavyoung.marketforge.infrastructure.persistent.po.RuleTreePO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 规则树主配置对象转换器。
 * <p>
 * 由 MapStruct 生成 Spring 管理的类型安全转换实现。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/07
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface RuleTreeAssembler {

    /**
     * 将规则树主表记录转换为尚未装配节点的领域值对象。
     *
     * @param ruleTreePO 规则树持久化对象
     * @return 规则树基础信息
     */
    @Mapping(target = "treeName", source = "treeDesc")
    @Mapping(target = "treeRootRule", source = "treeNodeRuleKey")
    @Mapping(target = "treeNodeVOMap", ignore = true)
    RuleTreeVO toVO(RuleTreePO ruleTreePO);
}
