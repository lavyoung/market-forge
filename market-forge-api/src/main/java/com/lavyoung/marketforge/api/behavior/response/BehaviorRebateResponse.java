package com.lavyoung.marketforge.api.behavior.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * 用户行为返利入账响应。
 * <p>
 * 一个用户行为可能命中多条返利配置，例如签到同时发放积分和活动 SKU。
 * 因此响应中返回订单号列表和事件号列表，便于调用方排查本次入账创建了哪些后续发放任务。
 *
 * @param orderIds 本次行为创建的返利订单号列表
 * @param eventIds 本次行为创建的可靠消息事件标识列表
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/10
 */
@Schema(description = "用户行为返利入账响应")
public record BehaviorRebateResponse(
        @Schema(
                description = "本次行为创建的返利订单号列表",
                example = "[\"RO202610100001\"]"
        )
        List<String> orderIds,
        @Schema(
                description = "本次行为创建的可靠消息事件标识列表",
                example = "[\"f9e9f5a2-5fd3-4c6e-a2b7-8a8b7c9f0001\"]"
        )
        List<String> eventIds
) implements Serializable {
}
