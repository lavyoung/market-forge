package com.lavyoung.marketforge.domain.strategy.model.entity;

import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import lombok.*;

/**
 * 规则过滤后的动作结果。
 *
 * @param <T> 规则执行阶段对应的结果数据类型
 * @author lavyoung
 * @version 1.0.0
 * @email lavyoung1325@outlook.com
 * @date 2026/09/04
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RuleActionEntity<T extends RuleActionEntity.RaffleEntity> {

    /**
     * 规则检查结果编码，默认表示放行。
     */
    private String code = RuleLogicCheckTypeVO.ALLOW.getCode();

    /**
     * 规则检查结果说明，默认使用放行说明。
     */
    private String msg = RuleLogicCheckTypeVO.ALLOW.getInfo();

    /**
     * 产生当前动作的规则模型编码。
     */
    private String ruleModel;

    /**
     * 规则执行阶段对应的业务数据。
     */
    private T data;

    /**
     * 各抽奖规则执行阶段结果的基础类型。
     */
    @Getter
    @Setter
    public static class RaffleEntity {

    }

    /**
     * 抽奖前规则的执行结果数据。
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    @Builder
    public static class RaffleBeforeEntity extends RaffleEntity {

        /**
         * 规则作用的策略标识。
         */
        private Long strategyId;

        /**
         * 命中的权重规则值，用于选择对应的概率查找表。
         */
        private String ruleWeightValueKey;

        /**
         * 规则直接指定的奖品标识。
         */
        private Long awardId;
    }

    /**
     * 抽奖执行中规则的结果数据。
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    @Builder
    public static class RaffleExecutingEntity extends RaffleEntity {

    }

    /**
     * 抽奖后规则的结果数据。
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    @Builder
    public static class RaffleAfterEntity extends RaffleEntity {

    }
}






