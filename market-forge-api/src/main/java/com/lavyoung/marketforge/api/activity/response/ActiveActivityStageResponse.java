package com.lavyoung.marketforge.api.activity.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 当前生效活动响应。
 *
 * @param activityId 当前渠道和来源对应的活动标识，以字符串返回以避免 JavaScript 精度丢失
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Schema(description = "当前生效活动")
public record ActiveActivityStageResponse(
        @Schema(description = "当前生效活动标识", example = "100301", type = "string")
        String activityId
) implements Serializable {
}
