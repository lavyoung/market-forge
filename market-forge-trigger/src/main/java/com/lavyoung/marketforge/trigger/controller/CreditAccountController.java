package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.credit.ICreditAccountApi;
import com.lavyoung.marketforge.api.credit.response.UserCreditAccountResponse;
import com.lavyoung.marketforge.application.credit.model.CreditAccountResult;
import com.lavyoung.marketforge.application.credit.service.ICreditAccountApplicationService;
import com.lavyoung.marketforge.trigger.assembler.CreditAccountResponseAssembler;
import com.lavyoung.marketforge.types.model.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户积分账户查询 HTTP 适配器。
 *
 * <p>该控制器实现积分账户查询 REST 契约，面向前端兑换页提供用户可用积分余额。
 * 它只承担协议适配职责，不直接访问数据库，也不执行积分调整。</p>
 *
 * <p>页面展示的可用积分只是查询时刻的余额快照。
 * 真正兑换时仍需由积分兑换应用服务按最新余额进行扣减校验。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/12
 */
@RestController
@RequiredArgsConstructor
public class CreditAccountController implements ICreditAccountApi {

    private final ICreditAccountApplicationService creditAccountApplicationService;
    private final CreditAccountResponseAssembler creditAccountResponseAssembler;

    @Override
    public Response<UserCreditAccountResponse> queryCreditAccount(String userId) {
        CreditAccountResult creditAccountResult = creditAccountApplicationService.queryCreditAccount(userId);
        return Response.success(creditAccountResponseAssembler.toResponse(creditAccountResult));
    }
}
