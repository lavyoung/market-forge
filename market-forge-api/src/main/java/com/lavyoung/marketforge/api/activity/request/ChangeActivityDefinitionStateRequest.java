package com.lavyoung.marketforge.api.activity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.io.Serializable;

/**
 * 变更活动定义状态请求。
 *
 * @param targetState     目标状态
 * @param expectedVersion 页面读取到的乐观锁版本
 */
@Schema(description = "变更活动定义状态请求")
public record ChangeActivityDefinitionStateRequest(
        @NotBlank(message = "目标状态不能为空")
        @Pattern(regexp = "^(open|close)$", message = "活动目标状态不合法") String targetState,
        @Min(value = 1, message = "期望版本不能小于 1") int expectedVersion
) implements Serializable {
}
