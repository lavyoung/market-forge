package com.lavyoung.marketforge.api.strategy.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 策略奖品配置请求项。
 *
 * <p>策略标识和剩余库存由服务端控制。客户端只提交奖品引用、展示信息、
 * 库存总量、概率权重和奖品级规则声明。</p>
 *
 * @param awardId       奖品业务标识
 * @param awardTitle    当前策略中的奖品标题
 * @param awardSubtitle 奖品副标题；可以为空
 * @param awardCount    奖品库存总量
 * @param awardRate     奖品概率权重
 * @param ruleModels    使用斜杠分隔的奖品级规则模型编码
 * @param sort          奖品展示顺序
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Schema(description = "策略奖品配置")
public record StrategyAwardConfigRequest(
        @Schema(description = "奖品业务标识", example = "900901011", type = "string", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "奖品标识不能为空") @Positive(message = "奖品标识必须为正数")
        Long awardId,

        @Schema(description = "奖品标题", example = "100积分", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "奖品标题不能为空") @Size(max = 128, message = "奖品标题长度不能超过 128 个字符")
        String awardTitle,

        @Schema(description = "奖品副标题", example = "中奖后自动到账")
        @Size(max = 128, message = "奖品副标题长度不能超过 128 个字符")
        String awardSubtitle,

        @Schema(description = "库存总量", example = "10000", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "奖品库存不能为空") @Positive(message = "奖品库存必须为正数")
        Integer awardCount,

        @Schema(description = "概率权重", example = "0.30000", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "奖品概率权重不能为空")
        @DecimalMin(value = "0.00001", message = "奖品概率权重必须大于零")
        @Digits(integer = 5, fraction = 5, message = "奖品概率权重最多包含 5 位整数和 5 位小数")
        BigDecimal awardRate,

        @Schema(description = "奖品级规则模型", example = "rule_lock/rule_stock")
        @Size(max = 64, message = "奖品规则模型长度不能超过 64 个字符")
        @Pattern(regexp = "^(?:rule_[a-z_]+(?:/rule_[a-z_]+)*)?$", message = "奖品规则模型格式不合法")
        String ruleModels,

        @Schema(description = "展示顺序", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "奖品顺序不能为空") @Positive(message = "奖品顺序必须为正数")
        Integer sort
) implements Serializable {
}
