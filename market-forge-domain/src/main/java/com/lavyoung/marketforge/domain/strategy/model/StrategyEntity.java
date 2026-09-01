package com.lavyoung.marketforge.domain.strategy.model;

import lombok.Builder;
import lombok.Data;

/**
 * 策略实体
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 * @date 2026/08/31
 */
@Data
@Builder
public class StrategyEntity {

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

    public String[] ruleModels() {

        // todo

        return new String[0];
    }
}
