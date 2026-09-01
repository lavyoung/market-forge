package com.lavyoung.marketforge.domain.strategy.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 策略奖品实体
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 */
@Data
@Builder
public class StrategyAwardEntity {

    /**
     * 抽奖策略id
     */
    private Long strategyId;
    /**
     * 奖品ID
     */
    private Long awardId;
    /**
     * 奖品标题
     */
    private String awardTitle;
    /**
     * 奖品副标题
     */
    private String awardSubtitle;
    /**
     * 奖品库存总量
     */
    private Integer awardCount;
    /**
     * 奖品库存剩余量
     */
    private Integer awardCountSurplus;
    /**
     * 奖品中奖概率
     */
    private BigDecimal awardRate;
    /**
     * 规则模型
     */
    private String ruleModels;
    /**
     * 奖品顺序
     */
    private Integer sort;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
