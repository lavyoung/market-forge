package com.lavyoung.marketforge.api.strategy.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.List;

/**
 * 权重奖池规则结构化配置请求。
 *
 * <p>一个权重规则由多个累计次数档位组成。档位门槛重复、奖品重复以及奖品是否
 * 属于当前策略奖池等聚合约束，继续由领域值对象和发布校验器统一处理。</p>
 *
 * @param tiers 权重奖池档位集合
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Schema(description = "权重奖池规则结构化配置")
public record WeightRuleConfigRequest(
        @Schema(description = "权重奖池档位", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "权重档位不能为空") @Size(max = 100, message = "权重规则最多配置 100 个档位")
        List<@NotNull(message = "权重档位不能为空") @Valid WeightTierConfigRequest> tiers
) implements Serializable {
}
