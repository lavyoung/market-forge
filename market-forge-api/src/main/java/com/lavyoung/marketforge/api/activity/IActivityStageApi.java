package com.lavyoung.marketforge.api.activity;

import com.lavyoung.marketforge.api.activity.response.ActiveActivityStageResponse;
import com.lavyoung.marketforge.types.model.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 用户端活动展台 REST API 契约。
 *
 * <p>用户页面通过渠道和来源解析当前活动，不再在前端配置固定活动编号。接口只返回
 * 当前活动标识，展台版本、审计信息和历史记录仍属于运营后台内部数据。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Tag(name = "活动展台", description = "根据流量渠道解析当前生效活动")
@RequestMapping(path = IActivityStageApi.BASE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public interface IActivityStageApi {

    /**
     * 用户端活动展台基础路径。
     */
    String BASE_PATH = "/activity/stages";

    /**
     * 查询当前生效活动。
     *
     * @param channel 投放渠道
     * @param source  流量来源
     * @return 当前生效活动标识
     * @throws com.lavyoung.marketforge.types.exception.BusinessException 当前没有生效活动时抛出
     */
    @Operation(operationId = "queryActiveActivity", summary = "查询当前生效活动")
    @GetMapping("/active")
    Response<ActiveActivityStageResponse> queryActiveActivity(
            @NotBlank(message = "投放渠道不能为空") @Pattern(regexp = "^[a-z][a-z0-9_-]{0,31}$", message = "投放渠道格式不合法") @RequestParam("channel") String channel,
            @NotBlank(message = "流量来源不能为空") @Pattern(regexp = "^[a-z][a-z0-9_-]{0,31}$", message = "流量来源格式不合法") @RequestParam("source") String source);
}
