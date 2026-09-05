package com.lavyoung.marketforge.domain.strategy.model.entity;

import lombok.Builder;

/**
 * 规则过滤所需的业务物料。
 *
 * @param userId     参与规则判断的用户标识
 * @param strategyId 规则所属的策略标识
 * @param awardId    规则所属的奖品标识；策略级规则可为空
 * @param ruleModel  待执行的规则模型编码
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/09/04
 */
@Builder
public record RuleMatterEntity(
        String userId,
        Long strategyId,
        Long awardId,
        String ruleModel
) {

}
