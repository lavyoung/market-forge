package com.lavyoung.marketforge.api.behavior.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * 用户行为返利入账请求。
 * <p>
 * 外部系统或前端在用户完成某个行为后调用该接口，例如用户签到、完成任务、浏览活动等。
 * 请求对象只表达入口协议字段，不直接暴露领域层枚举，避免调用方依赖内部领域模型。
 *
 * @param userId        发生行为的用户标识
 * @param behaviorType  用户行为类型编码，例如 {@code sign}
 * @param outBusinessNo 外部业务幂等号，例如签到日期、任务流水号或行为流水号
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/10
 */
@Schema(description = "用户行为返利入账请求")
public record BehaviorRebateRequest(
        @Schema(
                description = "发生行为的用户标识",
                example = "user-001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "用户标识不能为空")
        String userId,
        @Schema(
                description = "用户行为类型编码。当前支持sign 表示签到行为",
                example = "sign",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "用户行为类型不能为空")
        String behaviorType,
        @Schema(
                description = "外部业务编号，同一个用户、同一个行为、同一个外部业务编号入账一次",
                example = "sign-20261010-user-001",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "外部业务编号不能为空")
        String outBusinessNo
) implements Serializable {
}
