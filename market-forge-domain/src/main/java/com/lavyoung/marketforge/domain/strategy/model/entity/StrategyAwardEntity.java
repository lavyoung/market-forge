package com.lavyoung.marketforge.domain.strategy.model.entity;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 策略下参与抽奖的奖品配置。
 *
 * @param strategyId        抽奖策略标识
 * @param awardId           奖品标识
 * @param awardTitle        奖品标题
 * @param awardSubtitle     奖品副标题
 * @param awardCount        奖品库存总量
 * @param awardCountSurplus 奖品库存剩余量
 * @param awardRate         奖品中奖概率
 * @param ruleModels        奖品规则模型配置
 * @param sort              奖品顺序
 * @param createTime        创建时间
 * @param updateTime        更新时间
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 */
@Builder
public record StrategyAwardEntity(
        Long strategyId,
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
