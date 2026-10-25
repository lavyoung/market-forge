package com.lavyoung.marketforge.api.strategy.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 策略奖品配置响应。
 *
 * @param awardId           奖品业务标识，以字符串返回避免 JavaScript 精度丢失
 * @param awardTitle        奖品标题
 * @param awardSubtitle     奖品副标题
 * @param awardCount        库存总量
 * @param awardCountSurplus 剩余库存
 * @param awardRate         概率权重
 * @param ruleModels        奖品级规则模型
 * @param sort              展示顺序
 * @param createTime        创建时间
 * @param updateTime        最后修改时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Schema(description = "策略奖品配置")
public record StrategyAwardConfigResponse(
        @Schema(description = "奖品业务标识", example = "900901011", type = "string") String awardId,
        @Schema(description = "奖品标题", example = "100积分") String awardTitle,
        @Schema(description = "奖品副标题", example = "中奖后自动到账") String awardSubtitle,
        @Schema(description = "库存总量", example = "10000") Integer awardCount,
        @Schema(description = "剩余库存", example = "9999") Integer awardCountSurplus,
        @Schema(description = "概率权重", example = "0.30000") BigDecimal awardRate,
        @Schema(description = "奖品级规则模型", example = "rule_lock/rule_stock") String ruleModels,
        @Schema(description = "展示顺序", example = "1") Integer sort,
        @Schema(description = "创建时间", type = "string", format = "date-time") LocalDateTime createTime,
        @Schema(description = "最后修改时间", type = "string", format = "date-time") LocalDateTime updateTime
) implements Serializable {
}
