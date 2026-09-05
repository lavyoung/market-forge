package com.lavyoung.marketforge.domain.strategy.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规则物料实体类对象，用于过滤规则的必要参数信息
 *
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 * @date 2026/09/04
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleMatterEntity {

    /**
     * 参与规则判断的用户标识。
     */
    private String userId;

    /**
     * 规则所属的策略标识。
     */
    private Long strategyId;

    /**
     * 规则所属的奖品标识；策略级规则可为空。
     */
    private Long awardId;

    /**
     * 待执行的规则模型编码。
     */
    private String ruleModel;
}
