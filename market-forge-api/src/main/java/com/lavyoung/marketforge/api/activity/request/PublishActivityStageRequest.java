package com.lavyoung.marketforge.api.activity.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;

import java.io.Serializable;

/**
 * 发布活动展台请求。
 *
 * @param expectedVersion 客户端读取到的乐观锁版本
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Schema(description = "发布活动展台请求")
public record PublishActivityStageRequest(
        @Schema(description = "乐观锁版本", example = "1", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @Min(value = 1, message = "乐观锁版本不能小于 1")
        int expectedVersion
) implements Serializable {
}
