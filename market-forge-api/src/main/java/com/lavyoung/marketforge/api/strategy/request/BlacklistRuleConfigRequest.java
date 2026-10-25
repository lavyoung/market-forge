package com.lavyoung.marketforge.api.strategy.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.io.Serializable;
import java.util.List;

/**
 * 黑名单规则结构化配置请求。
 *
 * <p>命中 {@code userIds} 的用户将跳过普通概率抽奖，直接获得
 * {@code fallbackAwardId} 指定的兜底奖品。兜底奖品是否属于当前策略奖池，
 * 由领域发布校验器结合完整策略聚合判断。</p>
 *
 * @param fallbackAwardId 命中黑名单时返回的兜底奖品标识
 * @param userIds         需要执行兜底逻辑的用户标识集合
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Schema(description = "黑名单规则结构化配置")
public record BlacklistRuleConfigRequest(
        @Schema(description = "兜底奖品标识", example = "900901019", type = "string", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "黑名单兜底奖品不能为空") @Positive(message = "黑名单兜底奖品标识必须为正数")
        Long fallbackAwardId,

        @Schema(description = "黑名单用户标识", example = "[\"user-001\",\"user-002\"]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "黑名单用户不能为空") @Size(max = 1000, message = "黑名单用户最多配置 1000 个")
        List<@NotBlank(message = "黑名单用户标识不能为空") @Size(max = 64, message = "用户标识长度不能超过 64 个字符") String> userIds
) implements Serializable {
}
