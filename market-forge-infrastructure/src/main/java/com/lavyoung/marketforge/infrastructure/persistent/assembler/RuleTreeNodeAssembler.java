package com.lavyoung.marketforge.infrastructure.persistent.assembler;

import com.lavyoung.marketforge.domain.strategy.model.vo.RuleTreeNodeVO;
import com.lavyoung.marketforge.infrastructure.persistent.po.RuleTreeNodePO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 规则树节点对象转换器。
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
public interface RuleTreeNodeAssembler {

    /**
     * 将节点记录转换为尚未装配连线的领域值对象。
     *
     * @param treeNodePO 规则树节点持久化对象
     * @return 规则树节点基础信息
     */
    @Mapping(target = "ruleTreeNodeLineVoList", ignore = true)
    RuleTreeNodeVO toVO(RuleTreeNodePO treeNodePO);
}
