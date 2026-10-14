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
import com.lavyoung.marketforge.trigger.circuitbreaker.ActivityCircuitBreaker;
import com.lavyoung.marketforge.trigger.circuitbreaker.ActivityProtectionContext;
import com.lavyoung.marketforge.trigger.circuitbreaker.ActivityProtectionScene;
import com.lavyoung.marketforge.types.model.BusinessResponseCode;
import com.lavyoung.marketforge.types.model.Response;
import io.github.lavyoung.dcc.ratelimiter.annotation.DccRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RestController
@RequiredArgsConstructor
public class ActivityRaffleController implements IActivityRaffleApi {

    private final IActivityRaffleApplicationService activityRaffleApplicationService;
    private final ActivityRaffleResponseAssembler activityRaffleResponseAssembler;

    private final ActivityCircuitBreaker activityCircuitBreaker;

    @Override
    public Response<Void> armory(Long activityId) {
        activityRaffleApplicationService.armory(activityId);
        return Response.success();
    }

    /**
     * 执行用户活动抽奖。
     *
     * <p>首先由动态限流切面按用户标识判断是否允许访问；限流放行后，
     * 再由活动抽奖保护适配器执行超时控制和失败率熔断。</p>
     *
     * @param request 活动抽奖请求，包含用户、活动和活动 SKU
     * @return 抽奖结果；限流或系统保护触发时返回对应失败响应
     */
    @Override
    @DccRateLimiter(key = "userId", fallbackMethod = "drawRateLimiterFallback", permitsPerSecond = 1, blacklistCount = 3, blacklistTtlSeconds = 60L)
    public Response<ActivityDrawResponse> draw(ActivityDrawRequest request) {
        ActivityProtectionContext context = new ActivityProtectionContext(
                ActivityProtectionScene.DRAW,
                request.userId(),
                request.activityId()
        );
        return activityCircuitBreaker.execute(context, () -> executeDraw(request));
    }

    /**
     * 将抽奖请求转换为应用命令并组装抽奖响应。
     *
     * @param request 活动抽奖请求
     * @return 活动抽奖响应
     */
    private Response<ActivityDrawResponse> executeDraw(ActivityDrawRequest request) {
        ActivityRaffleCommand command = activityRaffleResponseAssembler.toCommand(request);
        ActivityRaffleResult result = activityRaffleApplicationService.raffle(command);
        return Response.success(activityRaffleResponseAssembler.toResponse(result));
    }

    /**
     * 处理活动抽奖被限流后的失败响应。
     *
     * <p>该方法参数列表必须与 {@link #draw(ActivityDrawRequest)} 保持一致，
     * 供当前 DCC 限流组件通过反射调用。该字符串 fallback 后续将按照
     * dynamic-config-center 中登记的增强任务迁移。</p>
     *
     * @param request 被限流的原始抽奖请求
     * @return 请求过于频繁的统一失败响应
     */
    private Response<ActivityDrawResponse> drawRateLimiterFallback(ActivityDrawRequest request) {
        log.info("活动抽奖触发用户限流 userId={} activityId={}", request.userId(), request.activityId());
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
