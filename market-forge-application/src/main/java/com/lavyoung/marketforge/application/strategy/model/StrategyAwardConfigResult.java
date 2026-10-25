package com.lavyoung.marketforge.application.strategy.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 策略奖品配置应用结果。
 *
 * <p>用于向 Trigger 返回策略中的完整奖品配置，同时避免外层直接依赖领域实体。</p>
 *
 * @param awardId           奖品业务标识
 * @param awardTitle        策略中的奖品标题
 * @param awardSubtitle     奖品副标题
 * @param awardCount        库存总量
 * @param awardCountSurplus 当前剩余库存
 * @param awardRate         概率权重
 * @param ruleModels        奖品级规则模型编码
 * @param sort              展示顺序
 * @param createTime        配置创建时间
 * @param updateTime        配置最后修改时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
public record StrategyAwardConfigResult(
        Long awardId,
        String awardTitle,
        String awardSubtitle,
        Integer awardCount,
        Integer awardCountSurplus,
        BigDecimal awardRate,
        String ruleModels,
        Integer sort,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
