package com.lavyoung.marketforge.domain.strategy.model.entity;

import com.lavyoung.marketforge.domain.strategy.model.vo.RuleLogicCheckTypeVO;
import lombok.Builder;

/**
 * 旧版规则过滤器的动作结果。
 * <p>
 * 当前抽奖前置规则使用责任链工厂的策略奖品结果，奖品命中后的规则使用规则树工厂的动作结果。
 * 保留本类型仅用于兼容仍依赖旧过滤器接口的代码。
 *
 * @param code      规则检查结果编码
 * @param msg       规则检查结果说明
 * @param ruleModel 产生当前动作的规则模型编码
 * @param data      规则执行阶段对应的业务数据
 * @param <T>       规则执行阶段对应的结果数据类型
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/04
 * @deprecated 前置规则请使用 {@link com.lavyoung.marketforge.domain.strategy.service.rule.chain.factory.DefaultChainFactory.StrategyAwardVO}，
 * 后置规则请使用 {@link com.lavyoung.marketforge.domain.strategy.service.rule.tree.factory.DefaultTreeFactory.TreeActionEntity}
 */
@Builder
@Deprecated(since = "1.0.0", forRemoval = false)
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




