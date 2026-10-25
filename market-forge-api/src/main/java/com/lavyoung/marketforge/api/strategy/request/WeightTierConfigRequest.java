package com.lavyoung.marketforge.api.strategy.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.io.Serializable;
import java.util.List;

/**
 * 权重规则的单个次数档位请求。
 *
 * <p>当用户累计完成次数达到 {@code threshold} 时，可以进入
 * {@code awardIds} 组成的子奖池。门槛允许为零，表示默认档位。</p>
 *
 * @param threshold 进入当前子奖池所需的最低累计次数
 * @param awardIds  当前档位包含的策略奖品标识
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Schema(description = "权重规则次数档位")
public record WeightTierConfigRequest(
        @Schema(description = "累计次数门槛", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "权重档位次数门槛不能为空") @PositiveOrZero(message = "权重档位次数门槛不能小于零")
        Long threshold,

        @Schema(description = "档位候选奖品标识", example = "[\"900901011\",\"900901012\"]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "权重档位候选奖品不能为空") @Size(max = 100, message = "单个权重档位最多包含 100 个奖品")
        List<@NotNull(message = "权重档位奖品标识不能为空") @Positive(message = "权重档位奖品标识必须为正数") Long> awardIds
) implements Serializable {
}
