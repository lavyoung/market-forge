package com.lavyoung.marketforge.infrastructure.persistent.po;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 抽奖策略持久化对象。
 *
 * @author lavyoung
 * @version 1.0.0-SNAPSHOT
 * @email lavyoung1325@outlook.com
 */
@Getter
@Setter
@NoArgsConstructor
public class StrategyPO {
    /**
     * 自增id
     */
    private Long id;
    /**
     * 策略id
     */
    private Long strategyId;
    /**
     * 描述
     */
    private String strategyDesc;
    /**
     * 规则模型
     */
    private String ruleModels;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
