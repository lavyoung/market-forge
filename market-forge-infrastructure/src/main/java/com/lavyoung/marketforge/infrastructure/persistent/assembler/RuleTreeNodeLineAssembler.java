package com.lavyoung.marketforge.infrastructure.persistent.assembler;

import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLimitTypeVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import com.lavyoung.marketforge.domain.strategy.model.vo.RuleTreeNodeLineVo;
import com.lavyoung.marketforge.infrastructure.persistent.po.RuleTreeNodeLinePO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 规则树节点连线对象转换器。
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
public interface RuleTreeNodeLineAssembler {

    /**
     * 将连线记录转换为规则树连线值对象。
     *
     * @param ruleTreeNodeLinePO 连线持久化对象
     * @return 完整的规则树连线
     * @throws IllegalArgumentException 限定类型或判断结果编码不受支持
     */
    @Mapping(target = "ruleLimitTypeVO", expression = "java(toLimitType(ruleTreeNodeLinePO.getRuleLimitType()))")
    @Mapping(target = "ruleLimitValue", expression = "java(toCheckType(ruleTreeNodeLinePO.getRuleLimitValue()))")
    RuleTreeNodeLineVo toVO(RuleTreeNodeLinePO ruleTreeNodeLinePO);

    /**
     * 将数据库限定类型编码转换为领域枚举。
     *
     * @param value 数据库编码
     * @return 对应限定类型
     * @throws IllegalArgumentException 编码为空或不受支持
     */
    default RuleLimitTypeVO toLimitType(String value) {
        for (RuleLimitTypeVO type : RuleLimitTypeVO.values()) {
            if (Integer.toString(type.getValue()).equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("unsupported rule limit type: " + value);
    }

    /**
     * 将数据库判断结果编码转换为领域枚举。
     *
     * @param value 数据库编码
     * @return 对应判断结果
     * @throws IllegalArgumentException 编码为空或不受支持
     */
    default RuleLogicCheckTypeVO toCheckType(String value) {
        for (RuleLogicCheckTypeVO type : RuleLogicCheckTypeVO.values()) {
            if (type.getCode().equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("unsupported rule check type: " + value);
    }

}
