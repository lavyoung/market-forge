package com.lavyoung.marketforge.trigger.assembler;

import com.lavyoung.marketforge.api.credit.request.CreditExchangeRequest;
import com.lavyoung.marketforge.application.credit.model.ExchangeCreditForSkuCommand;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 积分兑换接口层装配器。
 *
 * <p>负责将 API 层请求对象转换为 application 层命令对象。
 * 当前请求和命令均只携带用户、SKU、外部幂等号；
 * 积分价格必须由 application 层从服务端 SKU 配置中获取。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface CreditExchangeRequestAssembler {

    /**
     * 将积分兑换 HTTP 请求转换为应用层兑换命令。
     *
     * @param request 积分兑换 HTTP 请求
     * @return 积分兑换应用命令
     */
    ExchangeCreditForSkuCommand toCommand(CreditExchangeRequest request);
}
