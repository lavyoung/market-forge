package com.lavyoung.marketforge.api.activity.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 用户活动账户额度响应。
 *
 * <p>该对象用于前端活动抽奖页展示用户在指定活动下的抽奖次数。
 * 返回值同时包含总额度、日额度、月额度及其剩余额度，便于前端渲染不同维度的次数状态。</p>
 *
 * @param totalCount        累计获得的总抽奖次数
 * @param totalCountSurplus 当前剩余总抽奖次数
 * @param dayCount          当日可用抽奖次数
 * @param dayCountSurplus   当日剩余抽奖次数
 * @param monthCount        当月可用抽奖次数
 * @param monthCountSurplus 当月剩余抽奖次数
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/11
 */
@Schema(description = "用户活动账户额度")
public record ActivityAccountResponse(
        @Schema(description = "累计获得的总抽奖次数", example = "10")
        Integer totalCount,

        @Schema(description = "当前剩余总抽奖次数", example = "8")
        Integer totalCountSurplus,

        @Schema(description = "当日可用抽奖次数", example = "3")
        Integer dayCount,

        @Schema(description = "当日剩余抽奖次数", example = "2")
        Integer dayCountSurplus,

        @Schema(description = "当月可用抽奖次数", example = "10")
        Integer monthCount,

        @Schema(description = "当月剩余抽奖次数", example = "8")
        Integer monthCountSurplus
) implements Serializable {
}
