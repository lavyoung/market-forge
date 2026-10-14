package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.activity.IActivityRaffleApi;
import com.lavyoung.marketforge.api.activity.request.ActivityAwardListRequest;
import com.lavyoung.marketforge.api.activity.request.ActivityDrawRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityAccountResponse;
import com.lavyoung.marketforge.api.activity.response.ActivityAwardResponse;
import com.lavyoung.marketforge.api.activity.response.ActivityDrawResponse;
import com.lavyoung.marketforge.application.activity.model.*;
import com.lavyoung.marketforge.application.activity.service.IActivityRaffleApplicationService;
import com.lavyoung.marketforge.trigger.assembler.ActivityRaffleResponseAssembler;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.model.Response;
import io.github.lavyoung.dcc.ratelimiter.annotation.DccRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 活动抽奖 HTTP 适配器。
 * <p>
 * 实现活动抽奖 REST 契约，负责将 HTTP 请求转换为 application 命令，
 * 并把应用服务返回结果转换为统一响应对象。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/30
 */
@RestController
@RequiredArgsConstructor
public class ActivityRaffleController implements IActivityRaffleApi {

    private final IActivityRaffleApplicationService activityRaffleApplicationService;
    private final ActivityRaffleResponseAssembler activityRaffleResponseAssembler;

    @Override
    public Response<Void> armory(Long activityId) {
        activityRaffleApplicationService.armory(activityId);
        return Response.success();
    }

    /**
     * 执行用户活动抽奖。
     *
     * <p>以用户标识作为限流键，防止同一用户在短时间内重复发起抽奖。
     * 当请求超过许可速率时，限流组件不会进入 application 层，
     * 而是调用 {@link #drawRateLimiterFallback(ActivityDrawRequest)}
     * 返回统一的风控失败响应。</p>
     *
     * @param request 活动抽奖请求，包含用户、活动和活动 SKU
     * @return 抽奖成功结果；超过限流阈值时返回请求过于频繁
     */
    @Override
    @DccRateLimiter(key = "userId", fallbackMethod = "drawRateLimiterFallback", permitsPerSecond = 1, blacklistCount = 3, blacklistTtlSeconds = 60L)
    public Response<ActivityDrawResponse> draw(ActivityDrawRequest request) {
        ActivityRaffleCommand command = activityRaffleResponseAssembler.toCommand(request);
        ActivityRaffleResult raffle = activityRaffleApplicationService.raffle(command);
        return Response.success(activityRaffleResponseAssembler.toResponse(raffle));
    }

    /**
     * 处理活动抽奖被限流后的降级响应。
     *
     * <p>该方法的参数列表必须与 {@link #draw(ActivityDrawRequest)} 完全一致，
     * 限流组件会根据原方法参数类型反射查找并调用本方法。</p>
     *
     * <p>fallback 只负责协议层失败响应，不执行业务补偿，
     * 也不会调用 application 或 domain 层。</p>
     *
     * @param request 被限流的原始抽奖请求
     * @return 请求过于频繁的统一失败响应
     */
    private Response<ActivityDrawResponse> drawRateLimiterFallback(ActivityDrawRequest request) {
        return Response.fail(BusinessResponseCode.RISK_CONTROL_RATE_LIMITED);
    }

    @Override
    public Response<List<ActivityAwardResponse>> queryAwardList(ActivityAwardListRequest request) {
        ActivityAwardListCommand command = activityRaffleResponseAssembler.toCommand(request);
        List<ActivityAwardResult> results = activityRaffleApplicationService.queryAwardList(command);
        return Response.success(activityRaffleResponseAssembler.toAwardResponses(results));
    }

    @Override
    public Response<ActivityAccountResponse> queryActivityAccount(ActivityAwardListRequest request) {
        ActivityAwardListCommand command = activityRaffleResponseAssembler.toCommand(request);
        ActivityAccountResult accountResult = activityRaffleApplicationService.queryActivityAccount(command);
        return Response.success(activityRaffleResponseAssembler.toResponse(accountResult));
    }
}
