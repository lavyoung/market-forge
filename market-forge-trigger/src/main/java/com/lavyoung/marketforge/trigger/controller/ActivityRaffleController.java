package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.activity.IActivityRaffleApi;
import com.lavyoung.marketforge.api.activity.request.ActivityAwardListRequest;
import com.lavyoung.marketforge.api.activity.request.ActivityDrawRequest;
import com.lavyoung.marketforge.api.activity.response.ActivityAwardResponse;
import com.lavyoung.marketforge.api.activity.response.ActivityDrawResponse;
import com.lavyoung.marketforge.application.activity.model.ActivityAwardListCommand;
import com.lavyoung.marketforge.application.activity.model.ActivityAwardResult;
import com.lavyoung.marketforge.application.activity.model.ActivityRaffleCommand;
import com.lavyoung.marketforge.application.activity.model.ActivityRaffleResult;
import com.lavyoung.marketforge.application.activity.service.IActivityRaffleApplicationService;
import com.lavyoung.marketforge.trigger.assembler.ActivityRaffleResponseAssembler;
import com.lavyoung.marketforge.types.model.Response;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Response<Void> armory(Long activityId) {
        activityRaffleApplicationService.armory(activityId);
        return Response.success();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response<ActivityDrawResponse> draw(ActivityDrawRequest request) {
        ActivityRaffleCommand command = activityRaffleResponseAssembler.toCommand(request);
        ActivityRaffleResult raffle = activityRaffleApplicationService.raffle(command);
        return Response.success(activityRaffleResponseAssembler.toResponse(raffle));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response<List<ActivityAwardResponse>> queryAwardList(ActivityAwardListRequest request) {
        ActivityAwardListCommand command = activityRaffleResponseAssembler.toCommand(request);
        List<ActivityAwardResult> results = activityRaffleApplicationService.queryAwardList(command);
        return Response.success(activityRaffleResponseAssembler.toAwardResponses(results));
    }
}
