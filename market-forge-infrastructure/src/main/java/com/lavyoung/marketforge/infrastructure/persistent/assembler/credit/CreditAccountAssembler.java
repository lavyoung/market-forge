package com.lavyoung.marketforge.infrastructure.persistent.assembler.credit;

import com.lavyoung.marketforge.domain.credit.model.entity.CreditAccountEntity;
import com.lavyoung.marketforge.infrastructure.persistent.po.credit.UserCreditAccountPO;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 用户积分账户持久化对象与领域实体转换器。
 *
 * <p>该转换器负责隔离基础设施层的 MyBatis-Plus 持久化对象和领域层账户实体。
 * 领域层不依赖数据库主键、创建时间、更新时间等技术字段，只获取查询积分余额
 * 所需的用户标识、累计积分和可用积分。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/12
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CreditAccountAssembler {

    /**
     * 将用户积分账户持久化对象转换为领域实体。
     *
     * @param po 用户积分账户持久化对象；允许为 {@code null}
     * @return 用户积分账户领域实体；源对象为空时返回 {@code null}
     */
    CreditAccountEntity toEntity(UserCreditAccountPO po);
}
