package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.behavior.IBehaviorRebateApi;
import com.lavyoung.marketforge.api.behavior.request.BehaviorRebateRequest;
import com.lavyoung.marketforge.api.behavior.response.BehaviorRebateResponse;
import com.lavyoung.marketforge.application.behavior.model.BehaviorRebateOrderCreateResult;
import com.lavyoung.marketforge.application.behavior.model.CreateBehaviorRebateOrderCommand;
import com.lavyoung.marketforge.application.behavior.service.IBehaviorRebateApplicationService;
import com.lavyoung.marketforge.trigger.assembler.BehaviorRebateResponseAssembler;
import com.lavyoung.marketforge.types.model.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 用户行为返利 HTTP 适配器。
 * <p>
 * 该 Controller 实现用户行为返利 REST 契约，只负责三件事：
 * 接收 HTTP 请求、转换 application 命令、包装统一响应。
 * 返利配置查询、订单创建、消息任务保存等业务流程全部交给 application/domain/infrastructure 层完成。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/10
 */
@RestController
@RequiredArgsConstructor
public class BehaviorRebateController implements IBehaviorRebateApi {

    /**
     * 签到行为类型编码。
     *
     * <p>trigger 层不直接依赖 domain 层枚举，因此这里使用 API 入参约定中的字符串编码。</p>
     */
    private static final String SIGN_BEHAVIOR_TYPE = "sign";

    /**
     * 签到幂等号日期格式。
     */
    private static final DateTimeFormatter SIGN_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;

    private final IBehaviorRebateApplicationService behaviorRebateApplicationService;

    private final BehaviorRebateResponseAssembler behaviorRebateResponseAssembler;

    @Override
    public Response<BehaviorRebateResponse> createOrder(BehaviorRebateRequest request) {
        CreateBehaviorRebateOrderCommand command = behaviorRebateResponseAssembler.toCommand(request);
        BehaviorRebateOrderCreateResult result = behaviorRebateApplicationService.createOrder(command);
        return Response.success(behaviorRebateResponseAssembler.toResponse(result));
    }

    @Override
    public Response<BehaviorRebateResponse> calendarSignRebate(String userId) {
        String outBusinessNo = "sign-" + LocalDate.now().format(SIGN_DATE_FORMATTER) + "-" + userId;
        CreateBehaviorRebateOrderCommand command = new CreateBehaviorRebateOrderCommand(
                userId,
                SIGN_BEHAVIOR_TYPE,
                outBusinessNo
        );
        BehaviorRebateOrderCreateResult result = behaviorRebateApplicationService.createOrder(command);
        return Response.success(behaviorRebateResponseAssembler.toResponse(result));
    }
}
