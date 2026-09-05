package com.lavyoung.marketforge.domain.strategy.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 执行抽奖所需的输入因子。
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
public class RaffleFactorEntity {

    /**
     * 参与抽奖的用户标识。
     */
    private String userId;

    /**
     * 本次抽奖使用的策略标识。
     */
    private Long strategyId;
}
