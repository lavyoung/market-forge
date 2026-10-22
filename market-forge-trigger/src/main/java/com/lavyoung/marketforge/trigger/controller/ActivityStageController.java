package com.lavyoung.marketforge.trigger.controller;

import com.lavyoung.marketforge.api.activity.IActivityStageApi;
import com.lavyoung.marketforge.api.activity.response.ActiveActivityStageResponse;
import com.lavyoung.marketforge.application.activity.service.IActivityStageApplicationService;
import com.lavyoung.marketforge.types.model.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户端活动展台 HTTP 适配器。
 *
 * <p>根据渠道和来源解析当前活动标识，使用户前端无需固化活动编号。该控制器不承担
 * 活动发布、缓存预热或历史展台查询。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Validated
@RestController
@RequiredArgsConstructor
public class ActivityStageController implements IActivityStageApi {

    private final IActivityStageApplicationService activityStageApplicationService;

    @Override
    public Response<ActiveActivityStageResponse> queryActiveActivity(String channel, String source) {
        Long activityId = activityStageApplicationService.queryActiveActivityId(channel, source);
        return Response.success(new ActiveActivityStageResponse(activityId.toString()));
    }
}
