package com.lavyoung.marketforge.infrastructure.persistent.assembler.credit;

import com.lavyoung.marketforge.domain.credit.model.entity.CreditAccountOrderEntity;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountBusinessTypeVO;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountOrderStateVO;
import com.lavyoung.marketforge.domain.credit.model.vo.CreditAccountTradeTypeVO;
import com.lavyoung.marketforge.infrastructure.persistent.po.credit.UserCreditAccountOrderPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 积分账户订单持久化转换器。
 * <p>
 * 负责在积分账户订单领域实体和持久化对象之间转换。枚举字段在数据库中保存编码，
 * 避免持久化层暴露领域枚举结构。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CreditAccountOrderAssembler {

    /**
     * 将积分账户订单领域实体转换为持久化对象。
     *
     * @param source 积分账户订单领域实体
     * @return 忽略数据库生成字段后的积分账户订单持久化对象
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "tradeType", expression = "java(toCode(source.tradeType()))")
    @Mapping(target = "businessType", expression = "java(toCode(source.businessType()))")
    @Mapping(target = "state", expression = "java(toCode(source.state()))")
    UserCreditAccountOrderPO toPO(CreditAccountOrderEntity source);

    default String toCode(CreditAccountTradeTypeVO value) {
        return value.getCode();
    }

    default String toCode(CreditAccountBusinessTypeVO value) {
        return value.getCode();
    }

    default String toCode(CreditAccountOrderStateVO value) {
        return value.getCode();
    }

}
