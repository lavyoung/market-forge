package com.lavyoung.marketforge.domain.strategy.model.entity;

import lombok.Builder;

/**
 * 旧版规则过滤器所需的业务物料。
 * <p>
 * 当前责任链直接接收抽奖上下文，规则树统一接收 {@link RaffleFactorEntity}。
 * 保留本类型仅用于兼容仍依赖旧过滤器接口的代码。
 *
 * @param userId     参与规则判断的用户标识
 * @param strategyId 规则所属的策略标识
 * @param awardId    规则所属的奖品标识；策略级规则可为空
 * @param ruleModel  待执行的规则模型编码
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/04
 * @deprecated 前置规则应直接使用责任链上下文，后置规则请使用 {@link RaffleFactorEntity}
 */
@Builder
@Deprecated(since = "1.0.0", forRemoval = false)
public record RuleMatterEntity(
        String userId,
        Long strategyId,
        Long awardId,
        String ruleModel
) {

}
