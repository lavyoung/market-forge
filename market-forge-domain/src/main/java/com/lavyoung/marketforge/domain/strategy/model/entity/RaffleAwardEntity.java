package com.lavyoung.marketforge.domain.strategy.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 抽奖结果中的奖品信息。
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 * @date 2026/09/04
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RaffleAwardEntity {

    /**
     * 产生本次抽奖结果的策略标识。
     */
    private Long strategyId;
    /**
     * 奖品标识。
     */
    private Long awardId;
    /**
     * 奖品业务键。
     */
    private String awardKey;
    /**
     * 奖品发放配置。
     */
    private String awardConfig;
    /**
     * 奖品描述。
     */
    private String awardDesc;
}
