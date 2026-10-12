package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.credit.response.UserCreditAccountResponse;
import com.lavyoung.marketforge.application.credit.model.CreditAccountResult;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 用户积分账户响应装配器。
 *
 * <p>该装配器负责把 application 层积分账户查询结果转换为 API 响应对象。
 * 它只做字段投影，不负责查询账户、不处理账户不存在异常，也不执行积分扣减。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/12
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CreditAccountResponseAssembler {

    /**
     * 将积分账户应用查询结果转换为 API 响应。
     *
     * @param result 积分账户应用查询结果
     * @return 积分账户 API 响应
     */
    UserCreditAccountResponse toResponse(CreditAccountResult result);
}
