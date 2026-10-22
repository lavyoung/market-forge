package com.lavyoung.marketforge.api.award.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.io.Serializable;

/**
 * 变更奖品定义生命周期状态请求。
 *
 * <p>当前支持将草稿奖品启用，以及将已启用奖品停用。
 * 奖品标识由 HTTP 路径提供，操作人从当前登录会话取得。</p>
 *
 * <p>{@code expectedVersion} 用于乐观锁校验。即使应用层检查通过，
 * 持久化层仍需要把当前状态和版本作为更新条件，防止并发操作覆盖。</p>
 *
 * @param targetState     目标生命周期状态
 * @param expectedVersion 调用方最近一次查询到的乐观锁版本
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/22
 */
@Schema(description = "变更奖品定义生命周期状态请求")
public record ChangeAwardDefinitionStateRequest(
        @Schema(
                description = "目标状态",
                example = "active",
                allowableValues = {"active", "disabled"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "目标状态不能为空")
        @Pattern(regexp = "^(active|disabled)$", message = "目标状态只能是 active 或 disabled")
        String targetState,

        @Schema(
                description = "期望的乐观锁版本",
                example = "1",
                minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @Min(value = 1, message = "期望版本不能小于 1")
        int expectedVersion
) implements Serializable {
}
