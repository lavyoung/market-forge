package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.credit.ICreditExchangeApi;
import com.lavyoung.marketforge.api.credit.request.CreditExchangeRequest;
import com.lavyoung.marketforge.application.credit.model.ExchangeCreditForSkuCommand;
import com.lavyoung.marketforge.application.credit.service.ICreditExchangeApplicationService;
import com.lavyoung.marketforge.trigger.assembler.CreditExchangeRequestAssembler;
import com.lavyoung.marketforge.types.model.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * 积分兑换 HTTP 适配器。
 *
 * <p>该 Controller 实现积分兑换 REST 契约，只负责接收请求、
 * 转换 application 命令、调用应用服务并包装统一响应。
 * 积分扣减、余额校验、SKU 充值和事务一致性都不在 Controller 中处理。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
@RestController
@RequiredArgsConstructor
public class CreditExchangeController implements ICreditExchangeApi {

    private final ICreditExchangeApplicationService creditExchangeApplicationService;

    private final CreditExchangeRequestAssembler creditExchangeRequestAssembler;

    @Override
    public Response<String> exchangeCreditForSku(CreditExchangeRequest request) {
        ExchangeCreditForSkuCommand command = creditExchangeRequestAssembler.toCommand(request);
        return Response.success(creditExchangeApplicationService.exchangeCreditForSku(command));
    }
}
