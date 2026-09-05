package com.lavyoung.marketforge.domain.strategy.model.entity;

import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import lombok.Builder;

/**
 * 规则过滤后的动作结果。
 *
 * @param code      规则检查结果编码
 * @param msg       规则检查结果说明
 * @param ruleModel 产生当前动作的规则模型编码
 * @param data      规则执行阶段对应的业务数据
 * @param <T>       规则执行阶段对应的结果数据类型
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/04
 */
@Builder
public record RuleActionEntity<T extends RuleActionEntity.RaffleEntity>(
        String code,
        String msg,
        String ruleModel,
        T data
) {

    /**
     * 使用放行结果填充缺省的编码和说明。
     *
     * @param code      规则检查结果编码
     * @param msg       规则检查结果说明
     * @param ruleModel 产生当前动作的规则模型编码
     * @param data      规则执行阶段对应的业务数据
     */
    public RuleActionEntity {
        code = code == null ? RuleLogicCheckTypeVO.ALLOW.getCode() : code;
        msg = msg == null ? RuleLogicCheckTypeVO.ALLOW.getInfo() : msg;
    }

    /**
     * 各抽奖规则执行阶段结果的基础类型。
     */
    public sealed interface RaffleEntity
            permits RaffleBeforeEntity, RaffleExecutingEntity, RaffleAfterEntity {
    }

    /**
     * 抽奖前规则的执行结果数据。
     *
     * @param strategyId         规则作用的策略标识
     * @param ruleWeightValueKey 命中的权重规则值
     * @param awardId            规则直接指定的奖品标识
     */
    @Builder
    public record RaffleBeforeEntity(
            Long strategyId,
            String ruleWeightValueKey,
            Long awardId
    ) implements RaffleEntity {
    }

    /**
     * 抽奖执行中规则的结果数据。
     */
    @Builder
    public record RaffleExecutingEntity() implements RaffleEntity {
    }

    /**
     * 抽奖后规则的结果数据。
     */
    @Builder
    public record RaffleAfterEntity() implements RaffleEntity {
    }
}




